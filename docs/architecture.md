# Architecture

Atom Archetype generates a multi-module Maven project with DDD and ports-and-adapters boundaries. The sample `User` feature demonstrates those boundaries and can be replaced.

## Module dependencies

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

| Module | Owns | Excludes |
|---|---|---|
| `api` | Requests, responses, facade contracts, `AuthenticatedCaller` | Domain rules and persistence types |
| `domain` | Aggregates, value objects, policies, events, repository and service contracts | Spring, HTTP, MyBatis, and cache clients |
| `shared` | Result and error types shared across boundaries | Use cases and infrastructure helpers |
| `application` | Use cases, authorization, command/query execution, output ports | SQL, servlet types, and Redis implementations |
| `infra/rest` | HTTP transport, Spring Security, principal mapping, OpenAPI | Business invariants |
| `infra/persistence` | Repository adapters, PO conversion, MyBatis, Flyway, Redis adapters | Public API contracts |
| `infra/external` | Third-party output-port adapters | Application orchestration |
| `infra/security` | Password hashing and security adapters | Authentication policy and domain behavior |
| `infra/facade` | Facade contract implementations | Persistence details |
| `start` | Spring Boot entry point and runtime assembly | Reusable business logic |

`domain` has no dependency on `application`, `api`, `shared`, or any `infra` module. Infrastructure depends on the inward-facing contracts it implements.

## Request flow

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
                         ├─ check authority and TenantId
                         ├─ invoke domain behavior
                         └─ call a tenant-scoped repository or cache port
```

`AuthenticatedCaller` is server-side context. It is not deserialized from a request body or populated from client-controlled role headers. Application use cases check their required authority, and repository operations require a non-null `TenantId`.

The trusted-header filter is a development and test adapter. It is available only under `(dev | test) & !prod` and only when enabled. Production replaces the authentication adapter while keeping the same `AuthenticatedCaller` contract.

## Domain model

The sample `User` aggregate uses:

- validated value objects such as `UserId`, `TenantId`, `Username`, and `Email`;
- named behavior methods instead of public setters;
- factories for creation and `reconstitute(...)` for persistence restoration;
- domain-owned status and events;
- a persisted `version` for optimistic locking.

Persistence conversion restores state without creating domain events.

## Application operations and errors

Application use cases implement a `ServiceOperation<T>` lifecycle:

```text
validate → prepare → execute → onSuccess
```

`CommandServiceTemplate` adds rollback behavior for state-changing use cases. `QueryServiceTemplate` uses the same result and error mapping without command rollback behavior. Persistence runs in `execute`; commit-dependent work is registered from `onSuccess` through `AfterCommitExecutor`.

Domain failures use `DomainException` and `DomainError`. Application failures use `ApplicationException` or `NonRetryableApplicationException`. HTTP status mapping belongs to `infra/rest`.

## Transactions and events

The database transaction covers aggregate loading, domain behavior, and repository persistence. Cache changes and in-process event publication run after commit through `AfterCommitExecutor`.

The generated publisher is in-process only. Cross-service delivery requires a transactional outbox or another durable delivery mechanism. Outbox records and aggregate changes must be written in the same database transaction.

`LoggingUserNotificationAdapter` is available outside `prod`. A production deployment must provide a `UserNotificationPort` implementation.

## Persistence and cache

`domain.repository.UserRepository` defines the persistence port. `infra.persistence.repository.UserRepositoryImpl` implements it with tenant-scoped MyBatis queries.

Flyway migrations under `infra/persistence/src/main/resources/db/migration` are the schema source. Aggregate and PO versions provide optimistic locking; a zero-row update is reported as a concurrency conflict.

`application.port.out.CacheStore` is optional. Cache keys include tenant identity. With `atom.redis.enabled=false`, `NoOpCacheService` returns cache misses and persistence continues through MySQL.

`DistributedLock` is a coordination port. Database uniqueness and optimistic locking remain the consistency boundary.
