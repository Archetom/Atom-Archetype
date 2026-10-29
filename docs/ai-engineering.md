# AI engineering workflow

This workflow serves both developers and coding agents. It adds no application runtime dependency or agent service. The existing Maven modules and DDD contracts remain authoritative.

## Entry points and scope

Repository maintenance starts at the root [AGENTS.md](../AGENTS.md). Generated application work starts at [the application's guide](../src/main/resources/archetype-resources/AGENTS.md), with no separate generated `CLAUDE.md`; `llms.txt` indexes task-specific documents. Architecture and security rules are mandatory; domain-document organization is recommended; the shipped User model is an example.

The five application workflows are shipped under the template's `.agents/skills/`: [create-feature](../src/main/resources/archetype-resources/.agents/skills/create-feature/SKILL.md), [add-api](../src/main/resources/archetype-resources/.agents/skills/add-api/SKILL.md), [add-migration](../src/main/resources/archetype-resources/.agents/skills/add-migration/SKILL.md), [review-architecture](../src/main/resources/archetype-resources/.agents/skills/review-architecture/SKILL.md), and [verify-change](../src/main/resources/archetype-resources/.agents/skills/verify-change/SKILL.md). Maintainers may use them to understand application changes, but must validate templates from the archetype root. No duplicate repository Skills are needed for the existing root guide's maintainer workflow.

## Foundation audit and change map

| Existing capability | Gap addressed | Files and measurable benefit | Verification |
| --- | --- | --- | --- |
| Separate repository/application guides, documentation indexes, root Claude pointer | Application workflows were not packaged as Skills | Template `.agents/skills/*/SKILL.md` and guide/index links provide short task-specific entry points through `AGENTS.md` | Generation checks for presence, links, and unfiltered bytes |
| Detailed architecture, configuration, development, and testing guides | No place to distinguish business facts from assumptions | `docs/domain/README.md` and `user-example.md` in the generated project provide a small contract format with test references | Verify example claims against source/tests; retain useful guidance after cleanup |
| ArchUnit dependency checks and Maven Enforcer | Static constraints need regression coverage for boundary violations | Generated architecture tests remain the enforcement point; avoid a parallel source scanner | Fast generated suite and focused architecture rule tests |
| Caller, JWT, tenant, locking, converter, and transaction tests | These runtime guarantees cannot be proven by ArchUnit | Retain and run the existing JUnit/Spring/MySQL suites | Full scope with Docker |
| Wrapper, Make targets, archetype integration fixture, and CI | No shared scope/result interface; `demo-test` ran from the wrong directory | Makefiles and verification scripts delegate to the existing tools and return an explicit result | Script regression checks and real generated-project execution |
| Unfiltered guides and idempotent sample cleanup | New hidden Skills/scripts must be delivered and retained | Archetype metadata, generation assertions, and cleanup assertions protect delivery | Generate, compile, clean twice, then test the cleaned scaffold |

## Verification entry point

Run `make agent-check SCOPE=<scope>` from the repository or generated project root. Omitting `SCOPE` selects `full`; choose a smaller scope only when it matches the change. The entry point composes existing Maven/tests rather than replacing them.

| Scope | Archetype repository | Generated application |
| --- | --- | --- |
| `docs` | Staged/unstaged whitespace check; manually review facts and links | Same; without an independent Git root this scope is blocked and returns nonzero |
| `generate` | Generate via `make demo`, including official archetype integration tests that compile/test their generated fixture; no separate demo compile | Not supported |
| `fast` | Script regression checks, generation assertions and fast fixture tests, demo compilation, then cleanup twice and `CI=false clean test` on the disposable scaffold | `CI=false` Maven tests, including architecture tests; no Docker required |
| `full` | Script/generation/compile checks, Docker tests on the generated demo, then cleanup twice and `CI=true clean test` on the disposable scaffold | Fast tests first, then Docker tests with `CI=true`; does not remove the application's sample or business code |

Generated documentation changes still need `generate` even when no Java behavior changed. Check unfiltered output against source bytes, relative links, and unresolved Velocity markers. A successful `docs` check alone does not certify those properties or business accuracy.

The final JSON records context, scope, status, and checks; detailed command output is sent to standard error. Use the process exit code as well as the result. Missing Docker in a scope requiring it is blocked and returns nonzero. Passing `fast` explicitly leaves Docker integration behavior unverified. Root checks retain the temporary generated project on failure and report its location; successful checks remove it. Existing developer Maven/Make commands remain available for focused feedback.

Use the JDK and Maven versions pinned by the POM/wrapper. The maintainer-only process-contract tests use Python 3's standard library to validate JSON; generated applications need no Python. Never run an application's wrapper from the archetype directory expecting it to change Maven's working directory. See [root verification rules](../AGENTS.md#verification) and the [generated testing guide](../src/main/resources/archetype-resources/docs/test-guide.md) for contract-specific requirements.

## Keeping the foundation small

Keep rules at their existing enforcement layer: bytecode architecture tests for dependencies and API boundaries; JUnit for aggregate/caller/lifecycle behavior; MySQL integration tests for tenant SQL, migrations, and optimistic locking. Documentation links test names but does not replace executable assertions. Extend a Skill only when a recurring task needs project-specific guidance.

A future Lite architecture would need explicit decisions about module boundaries, the architecture tests, workflow module paths, and cleanup/generation expectations. This phase makes no Lite decision and introduces no alternate module graph. Its task workflows and domain-contract convention can be reused, but the verification contracts must change together with any future architecture.
