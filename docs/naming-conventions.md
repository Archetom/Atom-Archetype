# Naming conventions

Names are part of the architecture. They should reveal the business concept, the boundary, and whether a type carries behavior or data. Consistent names also make repository search, IDE navigation, generated API documentation, and AI-assisted maintenance more reliable.

## General Java rules

- Packages are lowercase and singular where practical: `domain.entity`, `application.service`, `infra.persistence`.
- Types use `UpperCamelCase`; methods and fields use `lowerCamelCase`; constants use `UPPER_SNAKE_CASE`.
- Prefer business language (`UserRegistrationPolicy`) over generic mechanics (`DataManager`, `CommonHelper`).
- Do not add `Impl` unless an interface and multiple boundary implementations make the distinction useful.
- Do not prefix interfaces with `I`.
- Do not use `Abstract` for a concrete class.
- Avoid catch-all `common`, `misc`, and `util` packages. Put a type with the concept that owns it.

## Boundary data types

| Suffix/name | Meaning | Example |
|---|---|---|
| `*Request` | External inbound API data | `UserCreateRequest` |
| `*Response` | External outbound API data | `UserResponse` |
| `AuthenticatedCaller` | Verified server-side actor, tenant, and authorities | `AuthenticatedCaller` |
| `*VO` | Application result/view data, never a persistence object | `UserVO` |
| `*PO` | Infrastructure persistence representation | `UserPO` |
| `PageResult<T>` | Framework-neutral domain/repository page | `PageResult<User>` |

Do not use a vague `DTO` suffix when `Request`, `Response`, command, query, or result expresses the role more precisely. Never bind `AuthenticatedCaller` from JSON or client-controlled role headers.

## Domain types

| Pattern | Rule | Example |
|---|---|---|
| Aggregate/entity | Use the ubiquitous-language noun | `User` |
| Value object | Use the represented concept, not `*ValueObject` | `TenantId`, `Email` |
| Domain service | Use `*DomainService` only for domain behavior that does not naturally belong to one aggregate/value object | `UserDomainService` |
| Policy | Use `*Policy` for a named business decision | `PasswordPolicy` |
| Repository port | Use aggregate name + `Repository` | `UserRepository` |
| Domain event | Past-tense business fact + `Event` | `UserCreatedEvent` |
| Domain error | Stable business concept | `DomainError`, `UserNotFoundException` |

Entities expose behavior with verbs such as `activate`, `lock`, `changeEmail`, and `delete`. Avoid general-purpose `setStatus` or public setters that bypass invariants.

Enums are named after the concept: `UserStatus`, `UseCaseOperation`. Do **not** append the redundant `Enum` suffix. Enum constants use `UPPER_SNAKE_CASE`.

## Application types

| Pattern | Meaning | Example |
|---|---|---|
| `*Service` | An application use-case boundary | `UserService` |
| `Command*` / `Query*` | Execution semantics for changing or reading state | `CommandServiceTemplate`, `QueryServiceTemplate` |
| `ServiceOperation<T>` | Typed lifecycle implemented by one use case | `ServiceOperation<UserVO>` |
| `*Mapper` | Mapping between error/security taxonomies when that is the class's sole responsibility | `DomainExceptionMapper` |
| `*Executor` | Executes a well-defined policy or lifecycle | `AfterCommitExecutor` |
| `*Store`, `*Publisher`, `*Lock` | Output port named for the capability, not a vendor | `CacheStore`, `DomainEventPublisher`, `DistributedLock` |

Application exceptions use the full names `ApplicationException` and `NonRetryableApplicationException`. Avoid abbreviations such as `AppException` and grammatically unclear names such as `AppUnRetryException`.

Use-case operation/error-scene codes live in `UseCaseOperation`; domain facts live in concrete `*Event` classes.

## Infrastructure types

| Suffix/name | Meaning | Example |
|---|---|---|
| `*Controller` | HTTP inbound adapter | `UserController` |
| `*FacadeImpl` | Implementation of a published facade contract | `UserFacadeImpl` |
| `*RepositoryImpl` | Repository adapter implementing a domain port | `UserRepositoryImpl` |
| `*Mapper` | MyBatis mapper or a narrowly scoped boundary mapper | `UserMapper` |
| `*Converter` | Domain ↔ persistence conversion | `UserPOConverter` |
| `*Config` | Spring configuration that assembles infrastructure | `SecurityConfig` |
| Vendor prefix | Use only on concrete adapters | `RedisCacheService`, `BCryptPasswordHasher` |

Do not introduce `Mapper → DAO → Repository` layers unless each adds a distinct policy. With MyBatis, a mapper plus a repository adapter is usually enough.

## Assemblers and converters

- `*Assembler` combines or reshapes objects at an application/API boundary.
- `*Converter` translates between domain and infrastructure representations.
- Conversion from PO to domain uses `reconstitute(...)` and restores identity/version without generating new domain events.
- Mapping methods state direction when it is not obvious: `toDomain`, `toPO`, `toResponse`.

Avoid a generic `convert(Object)` API that hides the source and target boundary.

## Methods and parameters

- Put required tenant scope near the start of a repository signature: `findById(TenantId tenantId, UserId id)`.
- Use `require*` for validation that throws: `requireUserId`.
- Use `find*` when absence is represented by `Optional`; use `get*` only when the value is expected to exist.
- Use `exists*` for boolean existence checks.
- Use past tense for recorded facts and imperative verbs for commands.
- Avoid boolean parameters whose meaning is unclear at the call site; prefer a policy/enum/value object.

## Configuration names

Project-owned configuration uses the `atom.*` namespace:

```text
atom.security.trusted-header.enabled
atom.security.trusted-header.authorities
atom.redis.enabled
```

Environment variables use Spring Boot's uppercase underscore form, for example `ATOM_REDIS_ENABLED`. Development-only headers use the explicit `X-Dev-*` prefix so they cannot be confused with a production identity contract.

## Test names

- Unit test class: subject + `Test`, for example `UserTest`.
- Adapter/container test: subject + `IntegrationTest`, for example `PersistenceIntegrationTest`.
- Test methods describe observable behavior: `rejectsCrossTenantLookup`, `detectsOptimisticLockConflict`.
- Avoid `test1`, `successCase`, or names tied only to implementation steps.

## Before adding a name

Search the repository first:

```bash
rg "class .*Repository|interface .*Repository" src/main/resources/archetype-resources
rg "UseCaseOperation|DomainError" src/main/resources/archetype-resources
```

If two names describe the same concept, choose one canonical term and migrate callers rather than creating synonyms.
