# Architecture and invariants

This project uses Domain-Driven Design with explicit ports and adapters. The objective is not to maximize layers; it is to keep business rules testable and prevent HTTP, database, cache, or authentication details from silently becoming domain rules.

## Dependency rule

Dependencies point toward the domain:

```text
infra/rest ───────┐
infra/facade ─────┼──> application ──> domain
infra/persistence ┘          │
                             └──> api and shared

infra/external ─────────────────────> application output ports
infra/security ─────────────────────> domain security ports
start ──────────────────────────────> all runtime adapters
```

The important constraints are:

- `domain` has no dependency on `api`, `application`, `shared`, or `infra`.
- `application` orchestrates use cases but does not contain MyBatis, servlet, or Redis code.
- Infrastructure modules implement inward-facing ports.
- `start` is the composition root; it contains no business rules.
- `shared` contains only small technical primitives. New domain concepts belong in `domain`.

## Module map

| Module | Owns | Must not own |
| --- | --- | --- |
| `api` | Public contracts and authenticated caller context | Domain behavior, persistence models |
| `domain` | Aggregates, value objects, policies, domain services, events, repository ports | Spring MVC, SQL, Redis, API DTOs |
| `application` | Use cases, authorization checks, transaction boundaries, output ports | HTTP parsing, mapper XML, vendor clients |
| `shared` | Result/error primitives used across boundaries | User or tenant business behavior |
| `infra/rest` | HTTP transport, authentication adapter, status mapping | Business decisions |
| `infra/persistence` | PO mapping, MyBatis queries, Flyway, cache implementations | Caller discovery, use-case orchestration |
| `infra/external` | Third-party output-port adapters | Application workflow |
| `infra/security` | Password hashing and security technology implementation | Domain policy |
| `infra/facade` | Public facade implementation and API/application mapping | Domain persistence |
| `start` | Runtime assembly and end-to-end tests | Reusable business logic |

## Request flow

```text
HTTP request
  -> Spring Security verifies or rejects identity
  -> AuthenticatedCallerMapper creates AuthenticatedCaller
  -> UserFacade maps the public contract and delegates to the application
  -> UserService selects CommandServiceTemplate or QueryServiceTemplate
  -> application derives TenantId and checks authority
  -> aggregate/domain service enforces business rules
  -> tenant-scoped repository port persists or queries
  -> facade maps the result to an API response
  -> REST adapter maps stable errors to HTTP status codes
```

`AuthenticatedCaller` is explicit method input. There is no domain `ThreadLocal`, so identity and tenant ownership remain visible in tests, async code, and call graphs.

## Command and query policies

`CommandServiceTemplate` is used for state changes. Its lifecycle is:

```text
validate -> prepare -> execute -> onSuccess
```

If a command catches an application or domain failure and converts it to a `Result`, the template marks the active transaction rollback-only.

`QueryServiceTemplate` uses the same type-safe `ServiceOperation` lifecycle without command rollback behavior. Do not use a command template for reads merely to reuse code.

## Aggregate persistence

The `User` aggregate owns its state transitions. Infrastructure reconstructs it through `User.reconstitute`, which restores identity, timestamps, tenant, and optimistic-lock version without raising new domain events.

Repository rules:

- every method requires a non-null `TenantId`;
- update predicates include tenant and aggregate identity;
- the persisted aggregate version is checked by MyBatis-Plus optimistic locking;
- zero updated rows are treated as a version conflict;
- save returns and synchronizes the same aggregate instance so pending events are preserved;
- physical delete is not part of the domain repository contract.

`UserStatus.DELETED` is the only soft-delete mechanism. Visibility of deleted users is an explicit query policy, not a hidden global SQL rewrite.

## Transactions, cache, and events

The relational database is the source of truth. Cache writes, cache eviction, and event publication are post-commit side effects coordinated by `AfterCommitExecutor`.

This prevents consumers from observing state that later rolls back. It does not provide durable event delivery: use a transactional outbox when an event must be delivered despite process failure after commit.

Redis is optional and implements an application output port. Cache keys include tenant identity. Disabling Redis selects a no-op adapter and must not change authorization or query correctness.

## Security invariants

- Anonymous requests to application APIs are rejected.
- Authorities are checked both in HTTP routing and at the application use-case boundary.
- Tenant identity comes from verified authentication context, never from request data.
- Production cannot install the trusted-header development adapter.
- Request headers cannot grant authorities or administrator state.
- Passwords, reset tokens, authentication credentials, and full request objects must not be logged.
- Repository and cache calls without a tenant fail closed.

See [configuration](configuration.md) for profile behavior and [usage guide](usage-guide.md) for adding a use case.

## Database evolution

Flyway is the only schema initialization mechanism. Migrations live under `infra/persistence/src/main/resources/db/migration` and use `V<version>__<description>.sql` names.

- Never combine Flyway with `schema.sql` or container init scripts.
- Never edit an applied migration.
- Test every migration against an empty MySQL database and, when relevant, a copy of the previous schema.
- Keep SQL columns, `UserPO`, converter mappings, mapper XML, and aggregate reconstruction in the same change.

## Architecture checklist

Before merging a change, verify:

- Can the domain behavior be tested without Spring?
- Is caller and tenant context explicit at every boundary?
- Does a state change run inside a command transaction?
- Are cache and event effects scheduled after commit?
- Does persistence restore and check the aggregate version?
- Is the database change represented by a new Flyway migration?
- Did any new infrastructure type leak into `domain`?
