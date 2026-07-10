# Architecture

Atom Archetype generates a layered Maven reactor with Domain-Driven Design and ports-and-adapters boundaries. The modules are deliberately more important than the sample `User` feature: replace the example while preserving the dependency rules.

## Design goals

1. Keep business rules testable without Spring, databases, HTTP, or Redis.
2. Make authenticated caller and tenant context explicit at application boundaries.
3. Treat databases and external services as replaceable adapters behind ports.
4. Keep transaction commit separate from non-transactional side effects.
5. Prefer one authoritative representation for each concern: one domain status, one schema migration chain, one error mapping path.

## Module dependency model

```text
                         ┌──────────────┐
HTTP ───────► infra/rest ───► │ application  │ ───► domain
Facade calls ► infra/facade ─►│ use cases    │
                         └──────┬───────┘
                                │ output ports
                ┌───────────────┬───────────────┐
                ▼               ▼               ▼
       infra/persistence  infra/external  infra/security

api     = published boundary contracts
shared  = result/error conventions
start   = composition root
```

| Module | Owns | Must not own |
|---|---|---|
| `api` | Requests, responses, facade contracts, `AuthenticatedCaller` | Domain rules, persistence types |
| `domain` | Aggregates, entities, value objects, policies, domain events, repository and service contracts | Spring components, HTTP types, MyBatis types, cache clients |
| `shared` | Stable result/error conventions shared across boundary modules | Business use cases or infrastructure helpers |
| `application` | Use cases, orchestration, authorization checks, command/query execution, output ports | SQL, servlet concerns, Redis implementation details |
| `infra/rest` | HTTP transport, Spring Security, principal mapping, OpenAPI, HTTP status mapping | Business invariants |
| `infra/persistence` | Repository adapters, PO conversion, MyBatis, Flyway, Redis adapters | Public API contracts |
| `infra/external` | Third-party adapters implementing application output ports | Application orchestration |
| `infra/security` | Password hashing and security technology adapters | Authentication policy or domain behavior |
| `infra/facade` | Published facade implementations | Domain persistence details |
| `start` | Runtime assembly and the Spring Boot entry point | Reusable business logic |

The hard invariant is: **`domain` never depends on `application`, `api`, `shared`, or any `infra` module.** Infrastructure can depend inward on the contracts it implements.

## HTTP request flow

For a user request, the generated path is:

```text
credential
   │
   ▼
Spring Security Authentication
   │ verified principal
   ▼
AuthenticatedCallerMapper
   │ actorId + tenantId + authorities
   ▼
Controller → Facade → Application service
                         │
                         ├─ validate authority and TenantId
                         ├─ invoke domain behavior
                         └─ call a tenant-scoped repository/cache port
```

`AuthenticatedCaller` is server-side context. Never deserialize it from a request body or populate it from untrusted role headers. Every use case checks its required authority, and every repository query receives a non-null `TenantId`.

The included trusted-header filter is only a local-development/test adapter. It is active only under `(dev | test) & !prod` and only when explicitly enabled. Production should replace the authentication mechanism while preserving the explicit application context.

## Domain model

The sample `User` is an aggregate root. Its important patterns are:

- validated value objects such as `UserId`, `TenantId`, `Username`, and `Email`;
- mutation through named business methods instead of public setters;
- factory methods for new aggregates and `reconstitute(...)` for persistence restoration;
- a domain-owned `UserStatus` rather than an API enum;
- pending domain events stored by `AggregateRoot`;
- a persistence version restored into the aggregate for optimistic concurrency.

Infrastructure conversion must not create business events. Conversely, aggregate creation/mutation must not depend on a PO, mapper, or Spring bean.

## Application operations and errors

Application use cases execute a typed `ServiceOperation<T>` lifecycle:

```text
validate → prepare → execute → onSuccess
```

`CommandServiceTemplate` adds transaction rollback behavior for state-changing use cases. `QueryServiceTemplate` applies the same result/error mapping without command rollback behavior. The lifecycle is intentionally small: persistence belongs in `execute`, and transaction-dependent side effects belong in `onSuccess` through `AfterCommitExecutor`.

Domain failures use `DomainException` and `DomainError`. The application maps them to its public error taxonomy. Application failures use `ApplicationException` or `NonRetryableApplicationException`; HTTP mapping belongs to `infra/rest`.

Do not return raw SQL, stack traces, or provider messages to clients. Add a stable domain/application error first, then map it at the transport boundary.

## Transactions and domain events

The database transaction covers aggregate loading, domain behavior, and repository persistence. Cache changes and event publication are registered with `AfterCommitExecutor` so they cannot announce data that later rolls back.

The generated publisher is suitable for in-process events and examples. It is **not a durable message-delivery guarantee**. If an event must survive process termination or be delivered to another service, add a transactional outbox:

1. persist aggregate changes and an outbox record in the same database transaction;
2. publish asynchronously after commit;
3. retry with idempotent consumers;
4. record delivery/poison-message state and operational metrics.

Clear pending aggregate events only after they have been handed to the selected reliable mechanism.

The generated `LoggingUserNotificationAdapter` is intentionally a non-production example and is active only when the `prod` profile is absent. A production deployment must provide a real `UserNotificationPort` implementation; otherwise startup fails fast instead of pretending a notification was delivered.

## Persistence and schema ownership

`domain.repository.UserRepository` is the port; `infra.persistence.repository.UserRepositoryImpl` is the MyBatis adapter. All methods require `TenantId`, and SQL applies tenant predicates unconditionally.

Flyway migrations under `infra/persistence/src/main/resources/db/migration` are the only schema source. Do not add competing Docker init SQL or test-only schema files. Production changes follow normal forward migration rules; never edit a migration that has already been deployed.

Optimistic locking uses the aggregate/PO `version` field. An update that affects no row is a concurrency conflict, not success. Reload the aggregate and retry only when the use case is safe to repeat.

The sample deletion model uses domain status rather than a second persistence-level logical-delete flag. Queries decide explicitly whether deleted aggregates are visible.

## Cache and distributed coordination

`application.port.out.CacheStore` is an optional performance port. Cache keys include tenant identity, and cached data must never bypass tenant authorization. When `atom.redis.enabled=false`, `NoOpCacheService` makes every lookup a miss and leaves business behavior unchanged.

The `DistributedLock` output port returns an owner-specific `LockHandle`. A distributed lock is coordination, not the final consistency boundary: database uniqueness and optimistic locking remain authoritative.

## Adding a bounded context

For a new context such as `orders`:

1. model aggregates, value objects, policies, errors, and repository ports in `domain`;
2. add use-case contracts and orchestration in `application`;
3. define public transport DTOs only when needed in `api`;
4. implement inbound adapters in `infra/rest` or `infra/facade`;
5. implement outbound ports in the relevant infrastructure module;
6. add Flyway migrations and adapter integration tests;
7. verify dependency direction with `sh ./mvnw dependency:tree` and tests.

Avoid a generic `common` package. A type should live with the concept that owns its lifecycle and invariants.
