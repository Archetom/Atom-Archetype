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
2. Add an application service method that accepts `AuthenticatedCaller`, checks authority, and derives `TenantId` before accessing data.
3. Use `CommandServiceTemplate` for state changes and `QueryServiceTemplate` for reads.
4. Call domain behavior from the application service; do not move the state transition into a controller or repository.
5. Define any required domain repository or security port, or application output port, then implement its adapter in the appropriate `infra` module.
6. Add or update the public Request, Response, and facade contract in `api`.
7. Bind the HTTP route and authentication details in `infra/rest`, then wire the adapter through `start`.

Commands use the `validate -> prepare -> execute -> onSuccess` lifecycle and run inside a transaction. Schedule cache changes and in-process event publication through `AfterCommitExecutor`. Use a transactional outbox when delivery must survive process failure after commit.

Queries also validate the caller and tenant before a repository or cache lookup. Every cache key must include tenant identity, and a cache hit must not bypass ownership checks.

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
