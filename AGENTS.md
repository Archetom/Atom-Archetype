# AGENTS.md

Atom Archetype is a Maven archetype for Java/Spring Boot applications with DDD boundaries. Complete the requested change, verify affected contracts (including generated output when applicable), and report any unverified behavior.

## Scope and sources

- For generated-application changes, edit `src/main/resources/archetype-resources/`. Java templates intentionally contain Velocity variables; `target/` is disposable output.
- The [generated application guide](src/main/resources/archetype-resources/AGENTS.md) owns application rules. Use this root guide's build commands for template work; application commands run in the generated project.
- Inspect nearby implementation and tests; use [llms.txt](llms.txt) to find docs relevant to the changed contract.
- Version sources: root/template `pom.xml`, `.mvn/wrapper/maven-wrapper.properties`, and template `docker-compose.yml`. Keep release history in `CHANGELOG.md`, not agent instructions.

## Execution and skills

- Complete authorized work; resolve routine choices from existing code. Ask when missing information changes scope, correctness, or authorization. Preserve unrelated work.
- Use `rg` for targeted discovery and batch independent reads. Delegate independent work when useful, with clear file ownership.
- Load skills when requested or relevant, reading references on demand. User instructions take precedence over skill guidance. If a skill blocks work, cite its `SKILL.md` and instruction, explain why, and continue unaffected work.
- Keep `CLAUDE.md` as a pointer. Project skills in `.agents/skills/` should cover specialized recurring tasks with narrow triggers, without duplicating these guides.
- Report changes, check results, and blockers concisely. Stop repeating successful checks unless new evidence warrants more work.

## Template editing

- Preserve `${groupId}`, `${artifactId}`, `${package}`, `${rootArtifactId}`, and `${version}` where Velocity filtering is enabled. To emit a literal Spring placeholder from filtered config, use `#set( $dollar = '$' )` and `${dollar}{ENV_VAR}`.
- Check `src/main/resources/META-INF/maven/archetype-metadata.xml` for inclusion, packaging, and filtering. Update it for modules or paths/types not already covered by a file set. `AGENTS.md`, `docs/`, scripts, and wrapper configuration are unfiltered; do not add Velocity escapes there.
- Update template `clean.sh` when removable example files or their configuration, tests, or references change. Generic framework files and documentation are retained. Validate cleanup on a disposable generated project.
- Keep affected docs and tests aligned with behavior, configuration, and public contracts. Add meaningful regression tests for behavior changes; prose-only edits do not need new Java tests.

## Application invariants

Preserve the generated guide's dependency boundaries, explicit caller/tenant scope, fail-closed authentication, aggregate reconstruction and optimistic locking, and independent after-commit effects. Flyway is the only schema source; Redis is optional. Profiles are explicitly selected and trusted dev/test headers cannot be enabled in prod. These are correctness requirements, not optional style preferences.

## Verification

Use the repository Maven wrapper and the JDK required by the POM. Select checks by the changed contract:

| Change | Required evidence |
| --- | --- |
| Repository docs or instruction/skill prose | Check facts, paths, links, and `git diff --check`; no Java/Docker suite needed. |
| Generated docs or instruction prose | Also generate and inspect the affected output for inclusion, links, and unresolved Velocity markers. Unfiltered files should match the source bytes. |
| Java templates, POMs, runtime config, metadata, or generation logic | Generate and compile using the commands below. `make demo` already runs clean/install and the official archetype integration test, whose generated project runs Maven `test`. |
| Database, authentication, transaction, mapper, cache, or event behavior | Also run `CI=true sh ./mvnw test` in the generated project with Docker available. |
| Sample removal or `clean.sh` behavior | Run `make clean-sample` twice in a disposable generated project, then its tests; use the cleanup assertions in `.github/workflows/ci.yml`. |

From the archetype repository root:

```bash
CI=false make demo
cd target/generated-projects/atom-demo
sh ./mvnw compile
```

`make demo` already depends on `make install`; do not run both separately. `DEMO_OUTPUT` overrides the output directory. Invoking the generated wrapper from the archetype root does not change Maven's working directory.

The archetype fixture/assertions are in `src/test/resources/projects/basic/`; full gates are in `.github/workflows/ci.yml`. Report commands and causes for blocked checks. For requested publishing, use [docs/releasing.md](docs/releasing.md).
