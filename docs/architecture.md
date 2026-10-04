# Architecture

Atom Archetype generates a multi-module Maven project with DDD and ports-and-adapters boundaries. The sample `User` feature demonstrates those boundaries and can be replaced.

## Module dependencies

```text
HTTP ──► infra/rest ─┐
gRPC ──► infra/grpc ─┴─► facades (api) ──► infra/facade ──► application ──► domain
                                                                 │ output ports
                                              ┌──────────────────┼──────────────────┐
                                              ▼                  ▼                  ▼
                                      infra/persistence   infra/external     infra/security

api     = published boundary contracts
shared  = result/error conventions
start   = composition root
```

| Module | Owns | Excludes |
|---|---|---|
| `api` | Requests, responses, and facade contracts published to HTTP and RPC clients | Identity parameters, domain rules, and persistence types |
| `domain` | Aggregates, value objects, policies, events, repository and service contracts | Spring, HTTP, MyBatis, and cache clients |
| `shared` | Result and error types shared across boundaries | Use cases and infrastructure helpers |
| `application` | Use cases, `AuthenticatedCaller`, authorization, command/query execution, output ports | SQL, servlet types, and Redis implementations |
| `infra/rest` | HTTP transport, Spring Security, OpenAPI | Business invariants |
| `infra/persistence` | Repository adapters, PO conversion, MyBatis, Flyway, Redis adapters | Public API contracts |
| `infra/external` | Third-party output-port adapters | Application orchestration |
| `infra/security` | Password hashing, `ActorPrincipal`, JWT claim mapping, and verified-caller resolution | Domain behavior and transport-specific wiring |
| `infra/facade` | Facade implementations served over HTTP and RPC | Persistence details and transport credentials |
| `infra/grpc` | Opt-in gRPC server: proto contracts, bearer-JWT authentication, status mapping over the facades | Use-case logic and direct application or domain access |
| `start` | Spring Boot entry point and runtime assembly | Reusable business logic |

`domain` has no dependency on `application`, `api`, `shared`, or any `infra` module. Infrastructure depends on the inward-facing contracts it implements.

## Request flow

```text
HTTP or RPC credential
   │ verified by the transport
   ▼
Spring Security context with ActorPrincipal
   │
   ▼
Controller or RPC endpoint → Facade (no identity parameters)
                               │ AuthenticatedCallerResolver: actorId + tenantId + authorities
                               ▼
                             Application service
                               ├─ check authority and TenantId
                               ├─ invoke domain behavior
                               └─ call a tenant-scoped repository or cache port
```

`AuthenticatedCaller` is server-side context. It is not part of the HTTP or RPC contract, is not deserialized from a request body or RPC argument, and is not populated from client-controlled role headers. Application use cases check their required authority, and repository operations require a non-null `TenantId`.

The trusted-header filter is a development and test adapter. It is available only under `(dev | test) & !prod` and only when enabled. Production authenticates HTTP and gRPC with bearer JWTs from a configured issuer; one converter maps the claims to `ActorPrincipal`, so the `AuthenticatedCaller` contract is the same for every transport.

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
validate → prepare → [resolveWithoutTransaction] → execute → onSuccess
```

`CommandServiceTemplate` runs validation and preparation before an independent transaction around `execute` and `onSuccess`. Failures roll back before they are converted to a `Result`; commit-dependent work is registered from `onSuccess` through `AfterCommitExecutor`. `QueryServiceTemplate` uses the same result and error mapping inside an independent read-only `REPEATABLE_READ` transaction. A result available without database work, such as a cache hit, is returned from `resolveWithoutTransaction` and never opens a transaction.

Domain failures use `DomainException` and `DomainError`. Value objects reject caller input with `InvalidValueException`; a bare `IllegalArgumentException` is treated as a programming error and maps to HTTP 500. Application failures use `ApplicationException` or `NonRetryableApplicationException`. HTTP status mapping belongs to `infra/rest`.

## Transactions and events

The command transaction covers domain mutation and repository persistence. Preparation may load a detached aggregate first; optimistic version checks reject concurrent changes. Cache changes and in-process event publication use separate post-commit registrations through `AfterCommitExecutor`.

The generated publisher is in-process only. Cross-service delivery requires a transactional outbox or another durable delivery mechanism. Outbox records and aggregate changes must be written in the same database transaction.

`LoggingUserNotificationAdapter` is available outside `prod`. A production deployment must provide a `UserNotificationPort` implementation.

## Persistence and cache

`domain.repository.UserRepository` defines the persistence port. `infra.persistence.repository.UserRepositoryImpl` implements it with tenant-scoped MyBatis queries.

Flyway migrations under `infra/persistence/src/main/resources/db/migration` are the schema source. Aggregate and PO versions provide optimistic locking; a zero-row update is reported as a concurrency conflict.

`application.port.out.CacheStore` is optional. Cache keys include tenant identity. With `atom.redis.enabled=false`, `NoOpCacheService` returns cache misses and persistence continues through MySQL.

Add coordination ports only when a concrete use case consumes them. Database uniqueness and optimistic locking remain the default consistency boundary.
