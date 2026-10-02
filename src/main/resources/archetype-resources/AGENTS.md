# AGENTS.md

This guide describes the generated application. Inside the archetype source tree, edit templates and follow the archetype root guide for verification. Run commands below from a generated project root.

## Working agreement

- Complete authorized work and relevant checks; preserve unrelated work. Resolve routine choices from existing contracts; ask when missing information changes the outcome or authorization.
- Inspect nearby code and tests; use [llms.txt](llms.txt) for relevant docs. Versions/commands come from POMs, `Makefile`, and wrapper config. Batch independent reads and delegate independent work when useful.
- Load requested or relevant skills on demand. User instructions take precedence over skill guidance. If a skill blocks work, cite its `SKILL.md` and instruction, explain why, and continue unaffected work.
- Update affected docs and regression tests for behavior changes. Report changes, check results, and missing coverage concisely; repeat successful checks only for new evidence.

## Architecture and naming

- `domain` owns aggregates, values, events, and repository ports. It depends on none of `api`, `application`, `shared`, `infra`, Spring, MyBatis, or Redis.
- `api` owns transport/facade contracts and `AuthenticatedCaller`; `shared` owns boundary conventions. Both are framework-neutral and independent of domain, application, and adapters.
- `application` owns use cases/output ports, depending on domain, api, and shared. `infra` implements adapters; `start` composes them. Controllers use use cases/facades, not repositories, and return API responses, not VOs.
- Names: API `*Request`/`*Response`, application `*VO`, persistence `*PO` extending `BasePO`, boundary mapping `*Assembler`, aggregate/PO mapping `*POConverter`, repository port/adapter `*Repository`/`*RepositoryImpl`.
- Application operation/error scenes use `application.operation.UseCaseOperation`; domain facts use `*Event`. See [docs/object-layering.md](docs/object-layering.md) for naming and ownership.
- `start` contains `ArchitectureBoundaryTest`, guarding dependencies, controllers, entities, and naming; include it when changing these boundaries.

## Security and tenancy

- Use cases receive `AuthenticatedCaller`, validate it through `CallerGuard`, and derive `TenantId` from verified caller context, not request data.
- Repository/cache operations require tenant scope and fail when absent; cache keys include tenant. Do not introduce identity/tenant ThreadLocals or fail-open queries.
- Trusted `X-Dev-User-Id`/`X-Dev-Tenant-Id` headers are restricted to the guarded dev/test adapter and cannot be enabled in prod. Never accept caller-controlled roles, authorities, or administrator headers such as `X-Admin`.
- Do not log passwords, tokens, reset codes, secrets, or sensitive request bodies. Log unexpected failures with `RedactedThrowable.of(exception)`, which keeps types and stack frames but never copies exception messages. Public errors expose stable codes and safe messages.
- Business REST endpoints use `/api/v1/`. Explicitly select runtime profiles from `conf/`; prod requires datasource environment variables. Keep test-only profiles in `start/src/test/resources` so they never ship in the jar. See [docs/configuration.md](docs/configuration.md).

## Domain, persistence, and transactions

- Aggregates expose business methods, not public setters or Lombok `@Data`. Create through factories; restore persisted aggregates through `reconstitute` with a named snapshot, restoring version without raising events.
- Repository save synchronizes the same aggregate instance to preserve collected events. Optimistic-lock conflicts must not overwrite newer data.
- In the User example, `UserStatus.DELETED` is the only soft-delete representation; do not add `@TableLogic` or `deleted_time` to it. `clean.sh` may remove this example while retaining these guides.
- Flyway is the only schema initializer. Add migrations; never edit applied ones. Keep schema, POs, converters, mapper XML, and reconstruction aligned. The database is authoritative; disabling Redis must preserve startup and correctness.
- Commands use `CommandServiceTemplate`; queries use `QueryServiceTemplate`. Both run `validate` and `prepare` before an independent transaction around `execute` and `onSuccess`. Queries use a read-only `REPEATABLE_READ` snapshot; commands use a write transaction. Serve cache hits from `resolveWithoutTransaction` so they never borrow a database connection.
- Register cache mutations and events separately through `AfterCommitExecutor`: rollback exposes neither, and one callback failure cannot suppress the other. Guaranteed cross-service delivery needs an outbox.
- Use domain-owned `DomainException`/`DomainError` and application-owned `ApplicationException`/`NonRetryableApplicationException`, mapped at boundaries. Value objects reject caller input with `InvalidValueException`; a bare `IllegalArgumentException` means a bug and maps to HTTP 500. Do not catch `Throwable` or suppress unexpected failures.

## Verification

Run from the generated project root with the JDK required by `pom.xml`:

```bash
# Focused feedback, including reactor dependencies (choose the affected module).
CI=false sh ./mvnw -pl application -am test

# All unit and architecture tests, without Docker.
CI=false sh ./mvnw test

# Includes Docker/MySQL Testcontainers integration tests.
CI=true sh ./mvnw test
```

- For prose-only edits, check accuracy, links, and `git diff --check`.
- Test affected behavior. Cross-module, dependency, or architecture changes require the full non-Docker suite, including `ArchitectureBoundaryTest`. Maven `test` already compiles.
- Database, authentication, transaction, mapper, cache, or event behavior changes also require the Docker suite. Set `CI` explicitly to control integration tests; report missing coverage if Docker/dependencies are unavailable.
- Test `clean.sh` changes on a disposable copy with `make clean-sample` twice, then run the cleaned project's tests. Preserve generic architecture/security building blocks.
- Do not weaken security, tenant isolation, or assertions to pass checks. See [docs/test-guide.md](docs/test-guide.md) for fixtures and coverage.
