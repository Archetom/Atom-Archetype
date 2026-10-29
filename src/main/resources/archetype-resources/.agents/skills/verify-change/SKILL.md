---
name: verify-change
description: Select and run checks for a change to this generated application or its archetype templates, reporting exact scope and unverified behavior.
---

# Verify a change

Locate the project root and read its `AGENTS.md` before running commands. A root with `src/main/resources/META-INF/maven/archetype-metadata.xml` is the archetype repository; this Skill lives in its template but does not change the working directory. Run application commands only from an actual generated project.

1. Inspect the full change, including new files and affected callers/configuration. Choose the union of required checks below. If the impact is uncertain, use `make agent-check` (default `SCOPE=full`); it requires Docker.
2. Get fast feedback with `CI=false sh ./mvnw -pl <affected-module> -am test` in a generated project. Then run the applicable shared entry point. Do not infer test success from log text; use its exit status and final JSON.

| Changed contract | Required evidence |
| --- | --- |
| Repository prose | Facts, local links, and `make agent-check SCOPE=docs`; this scope does not prove semantic accuracy |
| Generated prose/Skills | Also generate from the archetype root with `SCOPE=generate`; inspect inclusion, links, and byte preservation of unfiltered files |
| Template Java, POM, config, metadata, generation | Archetype root `SCOPE=fast`: generation, generated compilation, and fast tests |
| Domain behavior | Relevant domain tests; generated-project `SCOPE=fast` for cross-module or boundary impact |
| Application behavior | Caller/tenant and success/failure tests; full scope for transaction/event/cache/security changes |
| API contract | Validation, exact response/error mapping, OpenAPI and boundary tests; full scope when authorization/tenant behavior changes |
| Persistence/migration | Round-trip, tenant isolation, optimistic locking, migration/upgrade coverage, and full Docker scope |
| Security | Missing/invalid identity, forbidden capabilities, tenant isolation, guarded profiles, and full Docker scope |
| Transactions/events/cache | Commit, rollback, independent callbacks, cache fallback/tenant keys, and full Docker scope |
| Architecture/module dependencies | Generated-project `SCOPE=fast`, including `ArchitectureBoundaryTest`; runtime tests where behavior is affected |
| Sample cleanup | Disposable generated project, cleanup twice, retained generic files, then cleaned-project tests; archetype full scope supplies this flow |

3. Inspect failures and repair the relevant contract. Do not weaken assertions or security rules, edit disposable generated output as the final fix, or rerun successful suites without changed evidence.
4. Report commands, working directory, scope, results, and missing coverage. A passing `fast` result omits Docker tests. A blocked full check is not a pass; explain the dependency/Docker cause and the specific unverified behavior. See [testing guide](../../../docs/test-guide.md) for test landmarks.

Acceptance: required checks genuinely ran successfully, or each incomplete check is explicitly identified. No business assumption or skipped suite is presented as verified.
