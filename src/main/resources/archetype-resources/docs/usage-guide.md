# Development workflow

Use this guide when adding application behavior to a generated project. The dependency and security rules in [architecture](architecture.md) still apply.

## Local development loop

Start MySQL:

```bash
docker compose up -d mysql
```

Build the project, then run the `start` module with the `dev` profile and trusted development headers enabled:

```bash
sh ./mvnw clean install

ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Protected endpoints require both development identity headers:

```bash
curl \
  -H 'X-Dev-User-Id: 1' \
  -H 'X-Dev-Tenant-Id: 1' \
  http://localhost:8080/api/v1/users
```

Stop local services without deleting their data:

```bash
docker compose down
```

## Add a use case

Work from the domain toward the adapters:

1. Add the domain behavior, value objects, policy, and domain error.
2. Add the use-case/error-scene identifier to `application/operation/UseCaseOperation`.
3. Add an application service method that accepts `AuthenticatedCaller`, calls `CallerGuard.requireTenant(...)`, and never derives tenant identity from request data.
4. Use `CommandServiceTemplate` for state changes and `QueryServiceTemplate` for reads; call domain behavior from the application service, not a controller or repository.
5. Define only repository, security, or output-port methods consumed by the use case, then implement the adapter in the appropriate `infra` module.
6. Register framework-neutral factories, policies, and domain services in `application/config/DomainConfiguration` when they need runtime dependencies.
7. Add or update the public Request, Response, and facade contract in `api`.
8. Bind the HTTP route and its authority in `infra/rest/SecurityConfig`, then repeat the same capability check through `CallerGuard` in the use case.
9. Add the new local capability to both `conf/application-dev.yml` and `conf/application-test.yml`; otherwise the sample development identity will receive HTTP 403.
10. Add domain, application, persistence, REST, and integration tests as applicable. `ArchitectureBoundaryTest` must still pass.

Commands use the `validate -> prepare -> execute -> onSuccess` lifecycle. `validate` and `prepare` run before `CommandServiceTemplate` opens an independent transaction for `execute` and `onSuccess`, so CPU-heavy preparation does not occupy a database connection. Register event publication and cache work as separate `AfterCommitExecutor` actions. Use a transactional outbox when delivery must survive process failure after commit.

Queries also validate the caller and tenant before a repository or cache lookup. Every cache key must include tenant identity, and a cache hit must not bypass ownership checks.

### First additional aggregate: complete checklist

For an `Order` aggregate with one create command and one detail query, inspect or add each applicable item below. This list remains valid even after `clean.sh` removes the executable User sample:

| Area | Required work |
| --- | --- |
| `domain` | `Order`, `OrderId`, status/value objects, events/errors, `OrderRepository`, creation factory or method, and domain tests |
| `application` | `OrderService`, implementation, `OrderVO`, `OrderAssembler`, `UseCaseOperation` codes, `CallerGuard` checks, output ports, and application tests |
| domain wiring | Beans in `DomainConfiguration` for factories, policies, or services with constructor dependencies |
| `api` | Create/query Request types, `OrderResponse`, and `OrderFacade` contract |
| `infra/persistence` | `OrderPO`, `OrderPOConverter`, mapper, SQL/result map where names are exceptional, repository adapter, Flyway migration, and round-trip tests |
| `infra/facade` | Facade implementation using `ResultUtil.map(...)` for VO-to-Response mapping |
| `infra/rest` | Controller, caller mapping, route authority, validation, and HTTP tests |
| local security | Matching `orders:*` authorities in both dev and test YAML |
| `start` | Integration tests and any runtime adapter needed by the new output ports |
| documentation | Public API/configuration changes in docs and `llms.txt` |

Do not pre-populate speculative repository searches, locks, specifications, or adapters. Add a port when the first use case consumes it, and implement filtering in SQL rather than loading a tenant into memory.

### Expose an aggregate field update end to end

For a command such as “change user email,” make one coherent vertical change:

1. Add a validated `UserEmailUpdateRequest` and facade method in `api`.
2. Add `UserService.updateUserEmail(caller, userId, request)` and a `USER_EMAIL_UPDATE` operation code.
3. In `validate`, call `CallerGuard.requireTenant(caller, "users:write")`; in `prepare`, load the visible tenant-owned aggregate.
4. In transactional `execute`, call `user.changeEmail(...)` and save through `UserRepository`.
5. In `onSuccess`, pull events, register event publication, and independently register `userCacheService.invalidateUser(...)`.
6. Add the facade delegation, controller route, and the same `users:write` route rule in `SecurityConfig`.
7. Test the domain transition, caller/tenant rejection, persistence round trip, cache invalidation, HTTP contract, and optimistic-lock conflict.

This path does not require a new repository method: update the loaded aggregate and persist it through `save`.

## Add a repository operation

Define the port in `domain` and require the tenant explicitly:

```java
Optional<User> findByEmail(TenantId tenantId, String email);
```

Implement it in `infra/persistence` with a tenant predicate:

```java
LambdaQueryWrapper<UserPO> query = new LambdaQueryWrapper<>();
query.eq(UserPO::getTenantId, tenantId.getValue())
     .eq(UserPO::getEmail, email);
```

A missing tenant must fail closed. Do not interpret a null tenant as an unscoped or administrator query. Cross-tenant administration needs a separate port and authority.

Repository ports describe current use-case needs. Do not add bulk, time-window, specification, or lock methods in anticipation of future requirements.

For aggregate updates, leave the loaded `version` unchanged until MyBatis-Plus performs the compare-and-increment. Treat zero updated rows as an `AggregateVersionConflictException`.

## Add a database migration

Create the next migration in:

```text
infra/persistence/src/main/resources/db/migration
```

Use a versioned name such as:

```text
V2__add_user_last_login_time.sql
```

Update the migration, `UserPO`, converter mappings, mapper XML, aggregate reconstruction, and tests in the same change. Add the field to the aggregate and `reconstitute` only when it is domain state.

Flyway is the only schema source. Do not add `schema.sql`, Docker init SQL, or a test-only schema.

## Add an HTTP endpoint

Keep transport work in `infra/rest`:

1. Bind and validate Request data.
2. Receive Spring Security `Authentication` and map it with `AuthenticatedCallerMapper`.
3. Call the facade.
4. Map the result to an HTTP response.
5. Add route authorization in `SecurityConfig` and repeat the capability check in the application use case.

Do not accept actor ID, tenant ID, authorities, or administrator flags from an ordinary request body. Do not log passwords, tokens, or complete request objects that may contain them.

Use stable public errors and the narrowest internal failure type. Unexpected exceptions are logged internally and mapped to a generic response; stack traces, SQL, credentials, class names, and arbitrary exception messages are never returned.

## Verify the change

Run fast tests first, then the Docker-backed integration suite:

```bash
sh ./mvnw test
CI=true sh ./mvnw test
```

See [testing guide](test-guide.md) for the minimum test layer for each type of change.
