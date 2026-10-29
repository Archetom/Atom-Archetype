#!/usr/bin/env bash
# Maintainer-only process-contract tests; Python's standard library validates JSON.
set -euo pipefail
unset CI
repository=$(cd "$(dirname "$0")/.." && pwd)
script="$repository/src/main/resources/archetype-resources/scripts/agent-check.sh"
fixture=$(mktemp -d "${TMPDIR:-/tmp}/atom-agent-check-test.XXXXXX")
fixture=$(cd "$fixture" && pwd -P)
trap 'rm -rf -- "$fixture"' EXIT
mkdir -p "$fixture/bin" "$fixture/app/start" "$fixture/app/domain" \
    "$fixture/root/src/main/resources/META-INF/maven" "$fixture/root/scripts"
touch "$fixture/app/pom.xml" "$fixture/root/pom.xml" \
    "$fixture/root/src/main/resources/META-INF/maven/archetype-metadata.xml"
printf 'exit 0\n' > "$fixture/root/scripts/test-agent-check.sh"
cat > "$fixture/app/mvnw" <<'SH'
printf '%s|CI=%s|%s\n' "$PWD" "$CI" "$*" >> "$TRACE"
# Misleading log text must never control the result.
printf 'BUILD SUCCESS (fixture text)\n'
if [ "$CI" = true ]; then
    exit "${FAKE_INTEGRATION_EXIT:-0}"
fi
exit "${FAKE_MAVEN_EXIT:-0}"
SH
cat > "$fixture/bin/git" <<'SH'
#!/bin/sh
case "$1" in
    rev-parse) [ "${FAKE_GIT:-yes}" = yes ] || exit 1; pwd ;;
    diff) exit "${FAKE_GIT_EXIT:-0}" ;;
    *) exit 1 ;;
esac
SH
cat > "$fixture/bin/docker" <<'SH'
#!/bin/sh
printf 'docker|%s\n' "$*" >> "$TRACE"
exit "${FAKE_DOCKER_EXIT:-0}"
SH
cat > "$fixture/bin/make" <<'SH'
#!/bin/sh
printf '%s|make|CI=%s|%s\n' "$PWD" "${CI:-unset}" "$*" >> "$TRACE"
case "$*" in
    *demo*)
        for argument in "$@"; do
            case "$argument" in DEMO_OUTPUT=*) output=${argument#DEMO_OUTPUT=} ;; esac
        done
        cp -R "$APP_FIXTURE" "$output/atom-demo"
        mkdir -p "$output/atom-demo/scripts"
        printf 'exit "${FAKE_CLEAN_CHECK_EXIT:-0}"\n' > "$output/atom-demo/scripts/check-clean-sample.sh"
        ;;
    *clean-sample*) ;;
    *) exit 1 ;;
esac
exit "${FAKE_MAKE_EXIT:-0}"
SH
chmod +x "$fixture/bin/"*
export PATH="$fixture/bin:$PATH" TRACE="$fixture/trace" APP_FIXTURE="$fixture/app"
export FAKE_GIT=yes FAKE_GIT_EXIT=0 FAKE_MAVEN_EXIT=0 FAKE_DOCKER_EXIT=0 FAKE_MAKE_EXIT=0 FAKE_INTEGRATION_EXIT=0 FAKE_CLEAN_CHECK_EXIT=0

check() {
    local directory=$1 scope=$2 expected_code=$3 expected_status=$4 code=0
    : > "$TRACE"
    (cd "$directory" && bash "$script" "$scope") > "$fixture/report.json" 2> "$fixture/log" || code=$?
    if [ "$code" -ne "$expected_code" ]; then
        cat "$fixture/log" >&2
        printf 'Expected exit %s, got %s for %s\n' "$expected_code" "$code" "$scope" >&2
        exit 1
    fi
    python3 - "$fixture/report.json" "$expected_status" <<'PY'
import json, sys
with open(sys.argv[1]) as stream:
    result = json.load(stream)
assert result['status'] == sys.argv[2], result
assert result['checks'], result
if result['status'] == 'passed':
    assert all(check['status'] not in ('failed', 'blocked') for check in result['checks']), result
else:
    assert any(check['status'] in ('failed', 'blocked') for check in result['checks']), result
PY
}

reject_trace() {
    if grep -F "$1" "$TRACE" >/dev/null; then
        printf 'Unexpected command in trace: %s\n' "$1" >&2
        exit 1
    fi
}

check "$fixture/app" fast 0 passed
grep -F "$fixture/app|CI=false|-B -ntp test" "$TRACE" >/dev/null
reject_trace 'docker|'
# Repeating the command must still run verification and must not rely on an old report.
check "$fixture/app" fast 0 passed
test "$(wc -l < "$TRACE" | tr -d ' ')" = 1

FAKE_MAVEN_EXIT=17 check "$fixture/app" fast 17 failed
FAKE_DOCKER_EXIT=1 check "$fixture/app" full 2 blocked
reject_trace 'CI=true'
check "$fixture/app" full 0 passed
grep -F "$fixture/app|CI=false|-B -ntp test" "$TRACE" >/dev/null
grep -F "$fixture/app|CI=true|-B -ntp test" "$TRACE" >/dev/null
FAKE_INTEGRATION_EXIT=19 check "$fixture/app" full 19 failed
check "$fixture/app" '' 0 passed
python3 -c 'import json, sys; assert json.load(open(sys.argv[1]))["scope"] == "full"' "$fixture/report.json"
check "$fixture/app" 'fast"invalid' 2 failed
check "$fixture/app" generate 2 failed

check "$fixture/root" docs 0 passed
test ! -s "$TRACE"
FAKE_GIT_EXIT=9 check "$fixture/root" docs 9 failed
FAKE_GIT=no check "$fixture/app" docs 2 blocked

check "$fixture/root" generate 0 passed
grep -F '|make|CI=false|--no-print-directory demo DEMO_OUTPUT=' "$TRACE" >/dev/null
reject_trace '|CI=false|-B -ntp compile'
FAKE_MAKE_EXIT=8 check "$fixture/root" generate 8 failed
# Failed disposable output remains available for diagnosis; remove only the test fixture's path.
retained=$(sed -n 's/^Generated verification files retained at: //p' "$fixture/log")
test -n "$retained"
rm -rf -- "$retained"

check "$fixture/root" fast 0 passed
grep -F '/atom-demo|CI=false|-B -ntp compile' "$TRACE" >/dev/null
grep -F '/atom-demo|CI=false|-B -ntp clean test' "$TRACE" >/dev/null
test "$(grep -c '|make|CI=unset|--no-print-directory clean-sample' "$TRACE")" = 2
reject_trace 'docker|'
generated=$(sed -n 's/.*DEMO_OUTPUT=//p' "$TRACE")
test ! -e "$generated"

check "$fixture/root" full 0 passed
grep -F '/atom-demo|CI=true|-B -ntp test' "$TRACE" >/dev/null
grep -F '/atom-demo|CI=true|-B -ntp clean test' "$TRACE" >/dev/null
test "$(grep -c '|make|CI=unset|--no-print-directory clean-sample' "$TRACE")" = 2
FAKE_CLEAN_CHECK_EXIT=6 check "$fixture/root" fast 6 failed
reject_trace '/atom-demo|CI=false|-B -ntp clean test'
retained=$(sed -n 's/^Generated verification files retained at: //p' "$fixture/log")
test -n "$retained"
rm -rf -- "$retained"
printf 'agent-check process contracts passed.\n'
