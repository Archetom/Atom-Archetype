# AI engineering workflow

This workflow serves both developers and coding agents. It adds no application runtime dependency or agent service. The existing Maven modules and DDD contracts remain authoritative.

## Entry points and scope

Repository maintenance starts at the root [AGENTS.md](../AGENTS.md). Generated application work starts at [the application's guide](../src/main/resources/archetype-resources/AGENTS.md), with no separate generated `CLAUDE.md`; `llms.txt` indexes task-specific documents. Architecture and security rules are mandatory; domain-document organization is recommended; the shipped User model is an example.

The five application workflows are shipped under the template's `.agents/skills/`: [create-feature](../src/main/resources/archetype-resources/.agents/skills/create-feature/SKILL.md), [add-api](../src/main/resources/archetype-resources/.agents/skills/add-api/SKILL.md), [add-migration](../src/main/resources/archetype-resources/.agents/skills/add-migration/SKILL.md), [review-architecture](../src/main/resources/archetype-resources/.agents/skills/review-architecture/SKILL.md), and [verify-change](../src/main/resources/archetype-resources/.agents/skills/verify-change/SKILL.md). Maintainers may use them to understand application changes, but must validate templates from the archetype root. No duplicate repository Skills are needed for the existing root guide's maintainer workflow.

## Hardening audit and change map

| Existing capability | Gap addressed | Files and measurable benefit | Verification |
| --- | --- | --- | --- |
| Separate repository/application guides, five Skills, and documentation indexes | No comparable measure of task delivery | Repository-only `benchmarks/` separates tasks, development guidance, and independent acceptance; optional manual agent runs measure Skills' incremental value | Isolated-workspace and harness tests; controlled task runs when requested |
| Wrapper, scoped `agent-check`, and exit-code-based JSON | Every code change used broad tests; results lacked timings and report locations | An explicit `affected` scope conservatively selects existing Maven checks; additive report fields aid diagnosis | Scope-selection and process-result regression tests |
| Domain contract guide with a retained User example | A reference under `docs/domain/` could be mistaken for live application requirements after cleanup | Keep the guide in `docs/domain/README.md`; move reference material to `docs/examples/domain-contract.md` | Generate and check links/bytes; retain both after cleanup and reject the obsolete example path |
| ArchUnit rules with negative fixtures, Maven Enforcer, caller/JWT/tenant/locking/transaction tests | These already enforce the intended static and runtime contracts | Preserve them and the DDD module graph; no new production framework | Existing generated fast and Docker suites |
| Unfiltered Skills/scripts, generation assertions, and idempotent cleanup | New helper paths must remain packaged and usable | Extend existing delivery and cleanup assertions rather than add another build system | Generate, compile, clean twice, then test the cleaned scaffold |

## Verification entry point

Run `make agent-check SCOPE=<scope>` from the repository or generated project root. Omitting `SCOPE` selects `full`; choose a smaller scope only when it matches the change. The entry point composes existing Maven/tests rather than replacing them.

| Scope | Archetype repository | Generated application |
| --- | --- | --- |
| `docs` | Staged/unstaged whitespace check; manually review facts and links | Same; without an independent Git root this scope is blocked and returns nonzero |
| `generate` | Generate via `make demo`, including official archetype integration tests that compile/test their generated fixture; no separate demo compile | Not supported |
| `fast` | Script regression checks, generation assertions and fast fixture tests, demo compilation, then cleanup twice and `CI=false clean test` on the disposable scaffold | `CI=false` Maven tests, including architecture tests; no Docker required |
| `full` | Script/generation/compile checks, Docker tests on the generated demo, then cleanup twice and `CI=true clean test` on the disposable scaffold | Fast tests first, then Docker tests with `CI=true`; does not remove the application's sample or business code |
| `affected` | Repository-only prose selects `docs`; template prose/Skills selects `generate`; other changes conservatively select `full` | Recognized prose selects `docs`; eligible Java test changes in one domain/application module use Maven Reactor tests for that module and its dependencies; other changes select `full` |

Generated documentation changes still need `generate` even when no Java behavior changed. Check unfiltered output against source bytes, relative links, and unresolved Velocity markers. A successful `docs` check alone does not certify those properties or business accuracy.

The optional `affected` selector includes tracked/staged/unstaged changes, untracked files, deletions, and both rename paths. It does not classify production edits as semantically safe from filenames or absent keywords. Production code, POMs, public/shared contracts, cross-module changes, and security/tenant/persistence/transaction/cache/event-sensitive paths require full checks. Unknown paths, no independent Git root/history, or no detected changes also fall back to full. The requested scope remains visible alongside the selection and its reason. Default invocation remains `full`.

The final JSON preserves `status`, `context`, `scope`, and `checks`; selection, timing, exit-code, and test-report diagnostics are additive. Detailed command output goes to standard error. Use the process exit code as well as the result; a report path is diagnostic evidence, not a substitute for the command's outcome. Report metadata distinguishes absent reports from reports actually produced by the current invocation. Missing Docker in a scope requiring it is blocked and returns nonzero. Passing `fast` explicitly leaves Docker integration behavior unverified. Root checks retain the temporary generated project on failure and report its location; successful checks remove it. Existing developer Maven/Make commands remain available for focused feedback.

Use the JDK and Maven versions pinned by the POM/wrapper. Maintainer process-contract tests use Python 3's standard library. The optional `affected` selector also uses Python 3; when it is unavailable, selection falls back to full, while existing explicit scopes still need no Python. Never run an application's wrapper from the archetype directory expecting it to change Maven's working directory. See [root verification rules](../AGENTS.md#verification) and the [generated testing guide](../src/main/resources/archetype-resources/docs/test-guide.md) for contract-specific requirements.

## Optional task benchmark

The [benchmark guide](../benchmarks/README.md) owns setup, task prompts, acceptance, and results. It is an offline harness for three business tasks: adding an API, enforcing a new domain invariant, and evolving persistence. A developer can run the selected coding agent manually; no paid API, particular model, or agent runtime is required. Ordinary CI checks deterministic harness behavior, not probabilistic model performance. The harness does not belong in a generated application's production dependencies.

Use a separate disposable generated project for every task/mode, pinned to the same source revision. Keep task instructions separate from development hints and withheld acceptance tests; do not provide an implementation answer to the evaluated agent. Compare the same model/version and environment, with the same intervention policy.

Both **baseline** and **engineering** modes retain `AGENTS.md`. Baseline omits the additional Skills workflow, so this comparison measures the incremental effect of Skills, not the benefit of the entire AI engineering foundation. A claim about the complete foundation would require an additional control without its agent instructions.

Task success requires independent behavioral acceptance, not just compilation or an agent's own tests. Record first-pass results separately from later validation attempts, and keep environment-blocked or unexecuted runs distinct from failures. Report only observed duration, files, interventions, validation attempts, and token use; metrics that the run cannot observe remain `unavailable`. Without actual comparable agent runs, the harness provides an evaluation capability, not evidence that Skills improve speed or correctness.

## Keeping the foundation small

Keep rules at their existing enforcement layer: bytecode architecture tests for dependencies and API boundaries; JUnit for aggregate/caller/lifecycle behavior; MySQL integration tests for tenant SQL, migrations, and optimistic locking. Documentation links test names but does not replace executable assertions. Extend a Skill only when a recurring task needs project-specific guidance.

A future Lite architecture would need explicit decisions about module boundaries, the architecture tests, workflow module paths, and cleanup/generation expectations. This phase makes no Lite decision and introduces no alternate module graph. Its task workflows and domain-contract convention can be reused, but the verification contracts must change together with any future architecture.
