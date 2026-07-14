# Architecture

The generated project uses Domain-Driven Design with explicit ports and adapters. Business rules stay in `domain`; HTTP, persistence, cache, and authentication remain at the boundaries.

## Dependency rule

Dependencies point inward:

```text
infra/rest ───────┐
infra/facade ─────┼──> application ──> domain
infra/persistence ┘          │
                             └──> api and shared

infra/external ─────────────────────> application output ports
infra/security ─────────────────────> domain security ports
start ──────────────────────────────> all runtime adapters
```

- `domain` does not depend on `api`, `application`, `shared`, or `infra`.
- `application` contains use-case orchestration, not servlet, MyBatis, Redis, or vendor code.
- Infrastructure modules implement inward-facing ports.
- `start` is the composition root and contains no business rules.
- `shared` contains small technical primitives, not domain concepts.

## Modules

| Module | Owns | Must not own |
| --- | --- | --- |
| `api` | Public contracts and authenticated caller context | Domain behavior, persistence models |
| `domain` | Aggregates, value objects, policies, domain services, events, repository ports | Spring MVC, SQL, Redis, API DTOs |
| `application` | Use cases, authorization, transactions, output ports | HTTP parsing, mapper XML, vendor clients |
| `shared` | Boundary result and error primitives | User or tenant behavior |
| `infra/rest` | HTTP transport, authentication, status mapping | Business decisions |
| `infra/persistence` | PO mapping, MyBatis, Flyway, cache adapters | Caller discovery, use-case orchestration |
| `infra/external` | Third-party output-port adapters | Application workflow |
| `infra/security` | Password hashing and other security adapters | Domain policy |
| `infra/facade` | Facade implementation and API/application mapping | Persistence |
| `start` | Runtime assembly and end-to-end tests | Reusable business logic |

## Request flow

```text
HTTP request
  -> Spring Security verifies or rejects identity
  -> AuthenticatedCallerMapper creates AuthenticatedCaller
  -> UserFacade maps the public contract
  -> UserService selects CommandServiceTemplate or QueryServiceTemplate
  -> application checks authority and derives TenantId
  -> aggregate or domain service enforces business rules
  -> tenant-scoped repository queries or persists
  -> facade maps the result to an API response
  -> REST maps stable errors to HTTP status codes
```

`AuthenticatedCaller` is an explicit input. Identity and tenant ownership are not read from a domain `ThreadLocal`.

## Transactions and events

`CommandServiceTemplate` runs state changes through:

```text
validate -> prepare -> execute -> onSuccess
```

`CommandServiceTemplate` runs `validate` and `prepare` before opening an independent transaction for `execute` and `onSuccess`. Slow preparation such as password hashing therefore does not hold a database connection. A failure rolls that transaction back before it is converted to a `Result`, so an outer transaction is neither marked rollback-only nor surprised by `UnexpectedRollbackException`. Treat each command as its own atomic use case; do not use it when several commands must share one transaction. `QueryServiceTemplate` uses the same typed lifecycle without a transaction.

The relational database is the source of truth. `AfterCommitExecutor` delays cache writes, cache invalidation, and in-process event publication until commit. Events and cache actions use separate registrations so one callback failure cannot skip the other. Cache invalidation installs a short refill fence before eviction so a concurrent stale reader cannot repopulate a 30-minute entry after the write commits.

Events are already published from the post-commit phase. Use ordinary `@EventListener` consumers; an `@TransactionalEventListener(AFTER_COMMIT)` would be registered too late and will not receive this publication path.

Post-commit callbacks are not durable delivery. Use a transactional outbox when an event must survive process failure after commit.

Redis is optional and implements an application output port. Cache keys include tenant identity. Selecting the no-op adapter must not change authorization or query correctness.

## Aggregate persistence

`User` owns its state transitions. Persistence restores it through `User.reconstitute(UserSnapshot)`, including identity, tenant, timestamps, and optimistic-lock version, without raising new domain events. MapStruct creates the snapshot by field name, avoiding fragile long positional reconstruction calls.

Repository constraints:

- every operation requires a non-null `TenantId`;
- update predicates include tenant and aggregate identity;
- MyBatis-Plus compares and increments the persisted `version`;
- zero updated rows produce a version conflict;
- save synchronizes and returns the same aggregate instance so pending events remain available;
- physical deletion is not part of the domain repository contract.

`UserStatus.DELETED` is the only soft-delete mechanism. Deleted-user visibility is an explicit query policy, not a hidden SQL rewrite.

## Schema changes

Flyway is the only schema initializer. Migrations live in `infra/persistence/src/main/resources/db/migration` and use `V<version>__<description>.sql` names.

Do not add `schema.sql` or container initialization SQL, and do not edit an applied migration. Test migrations against an empty MySQL database and, when relevant, the previous schema. Keep migration columns, `UserPO`, converter mappings, mapper XML, and aggregate reconstruction in the same change.

## Security boundaries

- Application APIs reject anonymous requests.
- HTTP routes and application use cases both check authorities.
- Tenant identity comes from verified authentication, never ordinary request data.
- Production cannot install the trusted-header development adapter.
- Request headers cannot grant authorities or administrator state.
- Passwords, reset tokens, authentication credentials, and full request objects must not be logged.
- Repository and cache calls without a tenant fail closed.

See [configuration](configuration.md) for profile and security settings, and [development workflow](usage-guide.md) for adding behavior.

`ArchitectureBoundaryTest` uses ArchUnit bytecode analysis to enforce inward dependencies across application, domain, API, shared, and infrastructure classes. It also rejects controller-to-repository/VO coupling, public entity setters, missing core layers, and naming violations for Request, Response, VO, PO, and repository types.
