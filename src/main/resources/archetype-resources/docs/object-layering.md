# Object layering and naming

Names identify both meaning and architectural ownership. Prefer a domain or boundary term over generic names such as `Model`, `Data`, `Manager`, `Helper`, or `Util`.

## Object types

| Suffix or type | Owner | Purpose | Example |
| --- | --- | --- | --- |
| `*Request` | `api` | Public input contract and transport validation | `UserCreateRequest` |
| `*Response` | `api` | Public output contract | `UserResponse` |
| `AuthenticatedCaller` | `api` | Verified actor, tenant, and authorities | `AuthenticatedCaller` |
| `*VO` | `application` | Use-case output before facade mapping | `UserVO` |
| Aggregate/entity | `domain` | Identity, state, invariants, and behavior | `User` |
| Value object | `domain` | Immutable validated concept | `TenantId`, `Email`, `Username` |
| Domain enum | `domain` | Business state vocabulary | `UserStatus` |
| `PageResult<T>` | `domain` | Framework-neutral repository page result | `PageResult<User>` |
| `*PO` | `infra/persistence` | Relational persistence representation | `UserPO` |
| Domain event | `domain` | Immutable fact raised by an aggregate | `UserCreatedEvent` |

API Request/Response types and persistence objects do not belong in `domain`. Infrastructure never exposes `UserPO` through a public contract.

## Mapping path

```text
HTTP JSON -> Request -> application use case -> aggregate -> PO -> MySQL
MySQL -> PO -> reconstituted aggregate -> VO -> Response -> HTTP JSON
```

- `infra/rest` binds JSON and maps verified authentication to `AuthenticatedCaller`.
- `infra/facade` owns the public Request/Response boundary and maps VO to Response without exposing domain objects.
- `application` validates Request input, creates domain value objects, and uses an Assembler only for aggregate -> VO -> Response output mapping.
- `infra/persistence` maps aggregate to PO and calls `reconstitute` on reads.
- MapStruct handles mechanical field mapping only; validation, defaults, identity, version restoration, and event registration remain explicit.

## Creation and reconstruction

- A `create...` method or domain factory creates new state and may raise a creation event.
- `reconstitute(UserSnapshot)` restores persisted state by named fields and does not raise events.
- Methods such as `changeStatus`, `changeEmail`, and `delete` enforce state transitions.
- `onPersisted(...)` synchronizes generated identity, audit timestamps, and version after a successful write.

Aggregates do not expose public setters or use Lombok `@Data`. Define equality from aggregate identity or value-object value, not every mutable field.

`AuthenticatedCaller` stays at the API/application boundary. The application validates its tenant and passes a `TenantId` explicitly to domain, repository, and cache operations. Ordinary request bodies do not carry tenant identity, and repositories do not recover it from a `ThreadLocal`.

## Naming conventions

| Concern | Convention |
| --- | --- |
| Application write policy | `CommandServiceTemplate` |
| Application read policy | `QueryServiceTemplate` |
| Use-case lifecycle | `ServiceOperation<T>` |
| Public interface | Business name without an `I` prefix, such as `UserService` |
| Implementation | `*Impl` only for a meaningful interface |
| Output port | Business capability, such as `CacheStore` or `PasswordHasher` |
| Adapter | Technology plus capability when useful, such as `RedisCacheService` |
| API mapping | `*Assembler` or explicit static mapper methods |
| Persistence mapping | `*POConverter` |
| Repository port | `*Repository` in `domain` |
| Repository adapter | `*RepositoryImpl` in `infra/persistence` |
| Configuration | `*Config` or `*Configuration`, consistently within a package |
| Exception | Specific cause, not a catch-all name |
| Test | Class under test plus `Test`; integration intent visible in the name |

Avoid `Abstract*` on concrete Spring beans. Use `Request`, `Response`, `VO`, or a domain term instead of a generic `DTO` when the role is known.

## Error names

| Type | Meaning |
| --- | --- |
| `DomainException` | Stable domain failure with a domain error classification |
| `ApplicationException` | Application workflow failure that may be retried according to policy |
| `NonRetryableApplicationException` | Stable application rejection |
| Infrastructure exception | Translated at the adapter boundary or by a dedicated mapper |

Public errors expose a stable code and safe message, never an arbitrary internal exception message. Specific names include `UserNotFoundException`, `UserAlreadyExistsException`, and `AggregateVersionConflictException`.

## Persistence and collections

Java fields use camelCase and SQL columns use snake_case:

| Java | SQL |
| --- | --- |
| `tenantId` | `tenant_id` |
| `phoneNumber` | `phone_number` |
| `externalUser` | `is_external_user` |
| `passwordHash` | `password` (explicit legacy mapping) |
| `createdTime` | `created_time` |
| `updatedTime` | `updated_time` |

Persistence converters map every PO field in both directions, including `version` and audit timestamps. The optimistic-lock field is `version` in every layer. Soft deletion uses `status=DELETED`; there is no `deleted_time` field.

Repository single-result queries return `Optional<T>`, collections return an empty list rather than null, and domain pagination uses `PageResult<T>`. Convert it to public `Pager<T>` outside the domain boundary. Validate required input at the nearest trusted boundary and enforce domain invariants again in value objects.
