# Naming conventions

The generated project uses names to show layer ownership and object roles.

## Boundary types

| Name | Role | Example |
|---|---|---|
| `*Request` | External API input | `UserCreateRequest` |
| `*Response` | External API output | `UserResponse` |
| `AuthenticatedCaller` | Verified actor, tenant, and authorities | `AuthenticatedCaller` |
| `*VO` | Application result or view data | `UserVO` |
| `*PO` | Persistence representation | `UserPO` |
| `PageResult<T>` | Framework-neutral repository page | `PageResult<User>` |

Use `Request`, `Response`, command, query, or result instead of a generic `DTO` suffix when the boundary is known. `AuthenticatedCaller` is created from verified server-side authentication, not request JSON or client-controlled role headers.

## Domain types

| Pattern | Rule | Example |
|---|---|---|
| Aggregate or entity | Ubiquitous-language noun | `User` |
| Value object | Represented concept, without a `ValueObject` suffix | `TenantId`, `Email` |
| Domain service | `*DomainService` for behavior that belongs to no single aggregate or value object | `UserDomainService` |
| Policy | `*Policy` for a named business decision | `PasswordPolicy` |
| Repository port | Aggregate name + `Repository` | `UserRepository` |
| Domain event | Past-tense business fact + `Event` | `UserCreatedEvent` |
| Domain error | Stable business concept | `DomainError`, `UserNotFoundException` |

Entities expose behavior through names such as `activate`, `lock`, `changeEmail`, and `delete`. They do not expose general-purpose setters that bypass invariants.

Enums use the concept name, such as `UserStatus` and `UseCaseOperation`. Do not append `Enum`.

## Application types

| Pattern | Role | Example |
|---|---|---|
| `*Service` | Application use-case boundary | `UserService` |
| `Command*` / `Query*` | State-changing or read execution policy | `CommandServiceTemplate`, `QueryServiceTemplate` |
| `ServiceOperation<T>` | Typed lifecycle for one use case | `ServiceOperation<UserVO>` |
| `*Mapper` | Mapping between error or security taxonomies | `DomainExceptionMapper` |
| `*Executor` | A defined execution policy or lifecycle | `AfterCommitExecutor` |
| `*Store`, `*Publisher`, `*Lock` | Vendor-neutral output capability | `CacheStore`, `DomainEventPublisher`, `DistributedLock` |

Application exceptions use `ApplicationException` and `NonRetryableApplicationException`. Use-case operation codes live in `UseCaseOperation`; domain facts use concrete `*Event` classes.

## Infrastructure types

| Name | Role | Example |
|---|---|---|
| `*Controller` | HTTP inbound adapter | `UserController` |
| `*FacadeImpl` | Published facade implementation | `UserFacadeImpl` |
| `*RepositoryImpl` | Domain repository adapter | `UserRepositoryImpl` |
| `*Mapper` | MyBatis mapper or narrow boundary mapper | `UserMapper` |
| `*Converter` | Domain and persistence conversion | `UserPOConverter` |
| `*Config` | Spring infrastructure assembly | `SecurityConfig` |
| Vendor prefix | Concrete technology adapter | `RedisCacheService`, `BCryptPasswordHasher` |

With MyBatis, a mapper and repository adapter are normally sufficient. Add another persistence layer only when it owns a distinct policy.

## Assemblers and converters

- `*Assembler` maps or combines objects at an API or application boundary.
- `*Converter` translates between domain and infrastructure representations.
- PO-to-domain conversion calls `reconstitute(...)` and restores identity and version without creating events.
- Mapping methods state direction when needed: `toDomain`, `toPO`, `toResponse`.

## Methods and parameters

- Put tenant scope near the start of repository signatures: `findById(TenantId tenantId, UserId id)`.
- Use `require*` for validation that throws.
- Use `find*` when absence is represented by `Optional`.
- Use `exists*` for boolean existence checks.
- Use past tense for recorded facts and imperative verbs for commands.
- Replace unclear boolean parameters with a policy, enum, or value object.

## Configuration and tests

Project configuration uses the `atom.*` namespace:

```text
atom.security.trusted-header.enabled
atom.security.trusted-header.authorities
atom.redis.enabled
```

Environment variables use Spring Boot's uppercase underscore form, such as `ATOM_REDIS_ENABLED`. Development-only headers use the `X-Dev-*` prefix.

Unit test classes end in `Test`; adapter and container tests end in `IntegrationTest`. Test methods describe observable behavior, for example `rejectsCrossTenantLookup` and `detectsOptimisticLockConflict`.
