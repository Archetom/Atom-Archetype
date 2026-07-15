# AGENTS.md

This file is the operating guide for coding agents and automated contributors working in this generated repository.

## Start here

Read these files before changing code:

1. `README.md`
2. `docs/architecture.md`
3. `docs/object-layering.md`
4. the closest existing implementation and test

Use `llms.txt` as the compact documentation index.

## Build and test

```bash
sh ./mvnw clean install
sh ./mvnw test
CI=true sh ./mvnw test
```

`CI=true sh ./mvnw test` requires Docker and runs MySQL Testcontainers integration tests. After a database, security, transaction, mapper, cache, or event change, the integration suite is required.

Run focused tests with Maven reactor dependencies:

```bash
sh ./mvnw -pl shared -am test
sh ./mvnw -pl domain -am test
sh ./mvnw -pl application -am test
sh ./mvnw -pl infra/persistence -am test
sh ./mvnw -pl infra/rest -am test
```

## Architecture rules

- Dependencies point inward: adapters -> application -> domain.
- `domain` must not import API, application, servlet, Spring persistence, MyBatis, Redis, or infrastructure types.
- `application` owns use-case orchestration and output ports.
- `infra` implements ports and owns technology-specific code.
- `start` is the composition root, not a business layer.
- Keep `shared` small and free of domain concepts.
- Commands use `CommandServiceTemplate`; queries use `QueryServiceTemplate`.
- Keep `UseCaseOperation` in `application/operation`; names describe the application operation and its stable error-code scene.

## Security and tenancy rules

- Every use case receives an explicit `AuthenticatedCaller` and validates it through `CallerGuard`.
- Derive `TenantId` from verified caller context, never from normal request data.
- Every repository and cache operation is tenant scoped and fails when tenant is absent.
- Cache keys include tenant identity.
- Do not add identity or tenant ThreadLocals.
- Do not trust caller-controlled role, authority, administrator, actor, or tenant headers.
- `X-Dev-User-Id` and `X-Dev-Tenant-Id` are allowed only through the existing dev/test adapter.
- Production must remain unable to enable trusted headers.
- Never log passwords, authentication tokens, reset codes, secrets, or full sensitive requests.

## Domain and persistence rules

- Aggregates expose behavior methods, not public state setters.
- New aggregates use factories or creation methods; persisted aggregates use `reconstitute` with a named snapshot.
- Reconstitution restores version and never raises new events.
- Repository save synchronizes the same aggregate instance so events are preserved.
- Optimistic-lock conflicts must not overwrite newer data.
- `UserStatus.DELETED` is the only soft-delete representation; do not add `@TableLogic` or `deleted_time`.
- Flyway is the only schema initializer. Add a new migration; never edit an applied one.
- Keep migration, PO, converter, mapper XML, and aggregate reconstruction aligned.
- The relational database is the source of truth; Redis is optional.

## Transactions and events

- State changes run through `CommandServiceTemplate`, which opens an independent transaction around `execute` and `onSuccess`; `validate` and `prepare` run before that transaction.
- Cache mutation and event publication run through separate `AfterCommitExecutor` registrations.
- A rollback must not expose cache or event side effects.
- Use a transactional outbox for delivery guarantees beyond best-effort post-commit publication.
- Do not catch `Throwable` or suppress unexpected failures.
- Use `DomainException`, `ApplicationException`, and `NonRetryableApplicationException` according to ownership.
- Public errors expose stable codes and safe messages only.

## Naming

- API transport: `*Request`, `*Response`.
- Application output: `*VO`.
- Persistence model: `*PO`; persistence mapping: `*POConverter`.
- API mapping: `*Assembler`; persistence mapping: `*POConverter`.
- Ports use capability names such as `CacheStore` or `PasswordHasher`.
- Avoid `I*`, concrete `Abstract*` beans, and vague `Manager`, `Helper`, `Data`, or `Model` names.
- Preserve established package ownership when adding a class.

## Change workflow

1. Inspect the current contract and its callers with `rg`.
2. Make the smallest coherent change across all affected layers.
3. Add or update tests before considering the change complete.
4. Run focused tests, then the full fast suite.
5. Run Docker integration tests for boundary changes.
6. Update README, docs, and `llms.txt` when behavior, configuration, modules, or public contracts change.
7. Review the diff for secrets, sensitive logging, cross-tenant access, stale names, and duplicate configuration.

Do not weaken a security or tenant invariant merely to make a test pass. Fix the caller, fixture, or adapter so the invariant remains explicit.
