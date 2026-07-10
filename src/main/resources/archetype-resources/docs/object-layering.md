# Object layering and naming

Names in this project describe both meaning and architectural ownership. Avoid generic `Model`, `Data`, `Manager`, `Helper`, or `Util` names when a domain or boundary-specific term is available.

## Object types

| Suffix or type | Owner | Purpose | Example |
| --- | --- | --- | --- |
| `*Request` | `api` | Public input contract and transport validation | `UserCreateRequest` |
| `*Response` | `api` | Public output contract | `UserResponse` |
| `AuthenticatedCaller` | `api` | Verified actor, tenant, and authorities passed across the API boundary | `AuthenticatedCaller` |
| `*VO` | `application` | Use-case output before public facade mapping | `UserVO` |
| Aggregate/entity | `domain` | Identity, state, invariants, and behavior | `User` |
| Value object | `domain` | Immutable validated concept | `TenantId`, `Email`, `Username` |
| Domain enum | `domain` | Business state vocabulary | `UserStatus` |
| `PageResult<T>` | `domain` | Framework-neutral repository page result | `PageResult<User>` |
| `*PO` | `infra/persistence` | Relational persistence representation | `UserPO` |
| Domain event | `domain` | Immutable fact raised by an aggregate | `UserCreatedEvent` |

Do not expose `UserPO` from infrastructure or use API Request/Response types inside the domain.

## Mapping path

The normal flow is:

```text
HTTP JSON
  -> Request
  -> application arguments and domain value objects
  -> aggregate
  -> PO
  -> MySQL

MySQL
  -> PO
  -> reconstituted aggregate
  -> VO
  -> Response
  -> HTTP JSON
```

Mapping ownership:

- the REST adapter binds JSON and creates `AuthenticatedCaller` from verified authentication;
- the facade owns the public Request/Response boundary and invokes the mapping operations;
- the application validates public input, creates domain value objects, and maps aggregate output to VO;
- the facade maps the resulting VO to the public Response without exposing domain objects;
- the persistence adapter maps aggregate to PO and calls `reconstitute` on reads.

MapStruct may handle mechanical field mapping. Invariants, validation, defaults, identity assignment, version restoration, and event registration remain explicit code.

## Aggregate creation and reconstruction

Use clearly different methods:

- `create...` or a domain factory creates new business state and may raise a creation event;
- `reconstitute(...)` restores persisted state and must not raise events;
- behavior methods such as `changeStatus`, `changeEmail`, or `delete` enforce transitions;
- `onPersisted(...)` synchronizes generated identity, audit timestamps, and version after a successful write.

Avoid public setters and Lombok `@Data` on aggregates. Equality should be based deliberately on aggregate identity or value-object value, not all mutable fields by accident.

## Tenant and caller types

`AuthenticatedCaller` belongs at the public/application boundary. `TenantId` belongs in the domain.

```text
verified security principal
  -> AuthenticatedCaller
  -> validated TenantId
  -> repository/cache method argument
```

Do not add tenant IDs to user-controlled request bodies for ordinary tenant-scoped operations. Do not recover tenant identity from ThreadLocal state inside a repository.

## Naming conventions

| Concern | Convention |
| --- | --- |
| Application write policy | `CommandServiceTemplate` |
| Application read policy | `QueryServiceTemplate` |
| Type-safe use-case lifecycle | `ServiceOperation<T>` |
| Public interface | Business name without `I` prefix, for example `UserService` |
| Implementation | `*Impl` only when it implements a meaningful interface |
| Output port | Business capability, for example `CacheStore`, `PasswordHasher` |
| Adapter | Technology plus capability when useful, for example `RedisCacheService` |
| API mapping | `*Assembler` or explicit static mapper methods |
| Persistence mapping | `*POConverter` |
| Repository port | `*Repository` in domain |
| Repository adapter | `*RepositoryImpl` in persistence infrastructure |
| Configuration | `*Config` or `*Configuration`, one convention per package |
| Exception | Specific failure, not a catch-all name |
| Test | Class under test plus `Test`; integration intent visible in class name |

Avoid the `Abstract*` prefix on concrete Spring beans. Avoid `DTO` when `Request`, `Response`, `VO`, or a domain concept communicates the role more precisely.

## Error vocabulary

- `DomainException` represents a stable domain failure with a domain error classification.
- `ApplicationException` represents an application workflow failure that may be retried according to policy.
- `NonRetryableApplicationException` represents a stable application rejection.
- infrastructure exceptions are translated at the adapter boundary or by a dedicated mapper.
- public error responses expose a stable code and safe message, never an arbitrary internal exception message.

Names should describe the cause, such as `UserNotFoundException`, `UserAlreadyExistsException`, or `AggregateVersionConflictException`.

## Persistence naming

Java uses camelCase and SQL uses snake_case:

| Java | SQL |
| --- | --- |
| `tenantId` | `tenant_id` |
| `phoneNumber` | `phone_number` |
| `externalUser` | `is_external_user` |
| `createdTime` | `created_time` |
| `updatedTime` | `updated_time` |

The optimistic-lock field is named `version` in every layer. Soft deletion uses domain `status=DELETED`; there is no `deleted_time` field.

## Collection and absence rules

- Repository single-result queries return `Optional<T>`.
- Repository collections return an empty list, not null.
- `PageResult<T>` stays framework-neutral inside the domain boundary.
- Convert to the public `Pager<T>` only outside the domain.
- Validate required inputs at the closest trusted boundary and again in domain value objects for domain invariants.

## Review checklist

- Does the name reveal the object's owner and purpose?
- Did an API or persistence type leak into the domain?
- Is caller/tenant context explicit rather than hidden?
- Does reconstruction avoid validation side effects and new events?
- Are all PO fields mapped in both directions, including version and audit timestamps?
- Is a new suffix being introduced when an existing convention already fits?
