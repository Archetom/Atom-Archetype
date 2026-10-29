#!/usr/bin/env bash
# Assertions shared by CI and agent-check, after clean-sample runs on a disposable project.
set -euo pipefail

test -f pom.xml
test -d start
leftovers=$(find . -path '*/target' -prune -o -type f -name 'User*.java' -print)
if [ -n "$leftovers" ]; then
    printf 'User sample files remain after clean-sample:\n%s\n' "$leftovers" >&2
    exit 1
fi
test ! -e docs/api-reference.md
test -n "$(find . -path '*/target' -prune -o -type f -name DomainEventPublisherImpl.java -print)"
test ! -e docs/domain/user-example.md
for required in AGENTS.md llms.txt docs/domain/README.md docs/examples/domain-contract.md scripts/agent-check.sh scripts/affected-scope.py; do
    test -s "$required" || { printf 'Missing retained engineering file: %s\n' "$required" >&2; exit 1; }
done
for skill in create-feature add-api add-migration review-architecture verify-change; do
    test -s ".agents/skills/$skill/SKILL.md" || {
        printf 'Missing retained skill: %s\n' "$skill" >&2
        exit 1
    }
done
code=0
grep -R -n -E 'api-reference|/api/v1/users|users:(read|write|delete)' \
    README.md README.en.md AGENTS.md llms.txt infra/rest/src/main/java conf \
    start/src/test/resources || code=$?
if [ "$code" -eq 0 ]; then
    printf 'User-specific runtime configuration remains after clean-sample.\n' >&2
    exit 1
elif [ "$code" -ne 1 ]; then
    # A failed search is not evidence that the forbidden references are absent.
    exit "$code"
fi
printf 'Cleaned scaffold contracts passed.\n'
