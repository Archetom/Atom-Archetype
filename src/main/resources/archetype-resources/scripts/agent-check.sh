#!/usr/bin/env bash
# Shared by the archetype and generated applications. Invoke from the project root.
set -euo pipefail

scope=${1:-full}
context=unknown
status=failed
checks=()
temporary_project=
active_check=

record() {
    # Callers supply fixed identifiers and statuses, never command output or user text.
    checks+=("{\"name\":\"$1\",\"status\":\"$2\"}")
}

finish() {
    local code=$? separator= entry
    trap - EXIT
    if [ "$code" -ne 0 ] && [ -n "$active_check" ]; then
        record "$active_check" failed
    fi
    if [ "$code" -ne 0 ] && [ "$status" = passed ]; then
        status=failed
    fi
    if [ -n "$temporary_project" ]; then
        if [ "$code" -eq 0 ]; then
            rm -rf -- "$temporary_project" || {
                code=1
                status=failed
                record temporary-directory-cleanup failed
            }
        else
            printf 'Generated verification files retained at: %s\n' "$temporary_project" >&2
        fi
    fi
    printf '{"status":"%s","context":"%s","scope":"%s","checks":[' "$status" "$context" "$scope"
    for entry in "${checks[@]}"; do
        printf '%s%s' "$separator" "$entry"
        separator=,
    done
    printf ']}\n'
    exit "$code"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

run() {
    local name=$1 code=0
    shift
    active_check=$name
    printf '[agent-check] %s\n' "$name" >&2
    "$@" >&2 || code=$?
    active_check=
    if [ "$code" -eq 0 ]; then
        record "$name" passed
    else
        record "$name" failed
        return "$code"
    fi
}

case "$scope" in
    docs|generate|fast|full) ;;
    *) scope=unknown; record scope failed
       printf 'Expected SCOPE=docs, generate (archetype only), fast, or full.\n' >&2
       exit 2 ;;
esac

if [ -f src/main/resources/META-INF/maven/archetype-metadata.xml ] && [ -f pom.xml ]; then
    context=archetype
elif [ -f pom.xml ] && [ -f mvnw ] && [ -d start ] && [ -d domain ]; then
    context=application
else
    record project-root failed
    printf 'Run agent-check from the archetype or generated application root.\n' >&2
    exit 2
fi

if [ "$scope" = generate ] && [ "$context" != archetype ]; then
    record scope failed
    printf 'SCOPE=generate is only available in the archetype repository.\n' >&2
    exit 2
fi

check_whitespace() {
    git diff --check -- . && git diff --cached --check -- .
}

# A generated project nested in an archetype checkout must not check its parent's diff.
git_root=$(git rev-parse --show-toplevel 2>/dev/null || true)
if [ -n "$git_root" ] && [ "$(cd "$git_root" && pwd -P)" = "$(pwd -P)" ]; then
    run changed-file-whitespace check_whitespace
else
    record changed-file-whitespace skipped
    if [ "$scope" = docs ]; then
        record docs-change-context blocked
        status=blocked
        printf 'Docs checks need an independent Git project to identify changed files. Check links and facts manually.\n' >&2
        exit 2
    fi
fi

if [ "$scope" = docs ]; then
    printf 'Docs scope checks staged and unstaged whitespace only. Review links and facts; generated docs also require SCOPE=generate.\n' >&2
    status=passed
    exit 0
fi

require_docker() {
    if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
        record docker passed
    else
        record docker blocked
        status=blocked
        printf 'Docker is unavailable. Required integration tests have not passed; start Docker and rerun SCOPE=full.\n' >&2
        exit 2
    fi
}

application_test() {
    local directory=$1 integration=$2
    shift 2
    (cd "$directory" && CI="$integration" sh ./mvnw -B -ntp "$@" test)
}

clean_sample() {
    local directory=$1 integration=$2
    (cd "$directory" && make --no-print-directory clean-sample &&
        make --no-print-directory clean-sample && bash scripts/check-clean-sample.sh &&
        CI="$integration" sh ./mvnw -B -ntp clean test)
}

if [ "$context" = archetype ]; then
    if [ "$scope" != generate ]; then
        run verification-script-contract bash scripts/test-agent-check.sh
    fi
    temporary_project=$(mktemp -d "${TMPDIR:-/tmp}/atom-agent-check.XXXXXX")
    # make demo already installs the archetype and runs its official generated-project IT.
    run archetype-generation env CI=false make --no-print-directory demo "DEMO_OUTPUT=$temporary_project"
    project="$temporary_project/atom-demo"
    if [ "$scope" != generate ]; then
        run generated-compile bash -c 'cd "$1" && CI=false sh ./mvnw -B -ntp compile' _ "$project"
        if [ "$scope" = full ]; then
            require_docker
            run generated-integration-tests application_test "$project" true
            run cleaned-scaffold-tests clean_sample "$project" true
        else
            run cleaned-scaffold-tests clean_sample "$project" false
        fi
    fi
else
    run unit-and-architecture-tests application_test . false
    if [ "$scope" = full ]; then
        require_docker
        run integration-tests application_test . true
    fi
fi

if [ "$scope" != full ]; then
    record docker-integration-tests not-requested
    printf 'Docker integration tests were not requested by this scope.\n' >&2
fi
status=passed
