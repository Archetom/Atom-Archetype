# Upgrade guide

A Maven archetype generates source code once. It does not update an existing project. Generate a reference project from the target revision, compare it with the application, and apply the required changes.

## Available versions

| Source | Status | Java | Notes |
|---|---|---|---|
| Maven Central `2.2.0` | Current stable release | 25 | Bearer JWT authentication for HTTP and gRPC, optional gRPC server, RPC-safe facades |
| Maven Central `2.1.0` | Older release | 25 | First release on JDK 25 |
| Maven Central `2.0.0` / Git tag `v2.0.0` | Older release | 21 | First release of the 2.x architecture |
| Maven Central `1.1.0` | Published release | Legacy baseline | Spring Boot 3.5 architecture |

Generate a reference project from the target release on Maven Central. For `2.2.0`:

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.2.0 \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service-reference \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

## Changes in `2.2.0`

These changes alter generated-project contracts. Compare a project generated from `2.1.0` with a reference project generated from `2.2.0`.

| Area | `2.1.0` | `2.2.0` |
|---|---|---|
| Facade contract | `UserFacade` methods take `AuthenticatedCaller` | No identity parameters; `UserFacadeImpl` resolves the caller through `AuthenticatedCallerResolver` |
| Caller types | `api.context.AuthenticatedCaller`, `infra.rest.security.ActorPrincipal`, `AuthenticatedCallerMapper` | `application.security.AuthenticatedCaller`, `infra.security.ActorPrincipal`, `infra.security.AuthenticatedCallerResolver` |
| Invalid values | Value objects throw `IllegalArgumentException`, mapped to HTTP 400 | Caller-facing value objects throw `InvalidValueException` (HTTP 400); a bare `IllegalArgumentException` maps to HTTP 500 |
| Test profile | `conf/application-test.yml`, packaged into the application jar | `start/src/test/resources/application-test.yml`, test classpath only |
| Unexpected-failure logs | Exception type only | Full exception, including message and stack trace |
| RPC | None | Optional `infra/grpc` module: gRPC server disabled by default, bearer-JWT authentication over the facades |
| Production HTTP authentication | None; every API request returns 401 until you add an adapter | Bearer JWTs whenever a JWT decoder is configured, mapped by `ActorJwtAuthenticationConverter` (`atom.security.jwt.*`) |
| HTTP route authorities | Rules inside `SecurityConfig` | `ApiRouteAuthorization` beans, such as `UserRouteAuthorization`, sharing constants such as `UserAuthorities` with the use cases |
| Async executor | Custom `taskExecutor` bean, `task.executor.*` properties | Spring Boot's application task executor, `spring.task.execution.*` properties |
| Sample password column | `t_user.password` | `t_user.password_hash` |
| Local run | `-f start/pom.xml spring-boot:run` after `install` | `make run`, or `-pl start -am spring-boot:run`, from the current sources |
| Integration-test MySQL image | Hard-coded in `BaseIntegrationTest` | Read from `docker-compose.yml` |
| Guava | Transitive version | Managed at `33.7.2-jre`, which fixes GHSA-xxph-c9ww-hj94 |

1. Remove `AuthenticatedCaller` parameters from facade interfaces and implementations. Resolve the caller in the facade implementation with `AuthenticatedCallerResolver` and pass it to the use case; remove `Authentication` parameters and caller mapping from controllers.
2. Before serving a facade over RPC, authenticate every call in the RPC server adapter and put a verified `ActorPrincipal` into Spring Security's context on the invoking thread, as described under RPC exposure in the generated `docs/configuration.md`.
3. Update imports for the relocated caller types.
4. Throw `InvalidValueException` from value objects that validate caller input; keep `IllegalArgumentException` for values that never come from callers, such as `TenantId`.
5. Move `application-test.yml` to `start/src/test/resources`.
6. Treat logs as sensitive data now that they contain exception messages.
7. To authenticate HTTP with bearer JWTs, copy `infra/security/src/main/java/.../infra/security/jwt` and its POM dependencies, add `spring-boot-starter-security-oauth2-resource-server` to `infra/rest`, apply the `SecurityConfig` changes, and configure the issuer, for example with `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`.
8. To serve facades over gRPC, copy the `infra/grpc` module from the reference project, register it in the root and `start` POMs, add the gRPC settings from `conf/application.yml`, and configure a JWT decoder before setting `atom.grpc.enabled=true`.
9. Move the route rules out of `SecurityConfig` into one `ApiRouteAuthorization` bean per API area, keep the authority strings in one constants class per area, and use those constants in `CallerGuard` checks.
10. Replace `task.executor.*` with `spring.task.execution.*` (`thread-name-prefix`, `pool.core-size`, `pool.max-size`, `pool.queue-capacity`, `pool.keep-alive`). Delete the custom executor bean and `TaskExecutorProperties`, and keep the logging-context decorator as a `TaskDecorator` bean.
11. Keep an applied `V1` migration unchanged. To adopt the new column name, add a migration such as `ALTER TABLE t_user RENAME COLUMN password TO password_hash;` and update `UserPO` and the mapper XML in the same change.
12. To run `start` from the reactor, set `<skip>true</skip>` for `spring-boot-maven-plugin` in the root `pluginManagement` and `<skip>false</skip>` in `start`, then use `sh ./mvnw -pl start -am spring-boot:run`.
13. Manage `com.google.guava:guava` at `33.7.2-jre` or later in the root POM; earlier versions are affected by GHSA-xxph-c9ww-hj94.

## Major changes from `1.1.0`

| Area | `1.1.0` | 2.x architecture |
|---|---|---|
| Runtime | Spring Boot 3.5 | Spring Boot 4; JDK 21 on `2.0.0`, JDK 25 from `2.1.0` |
| Caller context | Domain `UserContextHolder` | Explicit `AuthenticatedCaller` passed to use cases |
| Tenant scope | Header/ThreadLocal-derived | Validated `TenantId` passed to repositories and caches |
| Development identity | `X-User-Id`, `X-Tenant-Id`, `X-Admin` | `X-Dev-User-Id`, `X-Dev-Tenant-Id`; dev/test only and explicitly enabled |
| HTTP security | Legacy permissive paths | Authentication and per-operation authorities |
| Profiles | `dev` activated implicitly | `SPRING_PROFILES_ACTIVE` is required |
| Schema | Multiple SQL initialization paths | Flyway migrations only |
| Persistence | Legacy entity mapping | PO conversion, aggregate reconstitution, optimistic-lock version |
| Redis | Infrastructure assumed available | Optional adapter selected by `atom.redis.enabled` |
| Operations | Callback templates | `CommandServiceTemplate`, `QueryServiceTemplate`, `ServiceOperation<T>` |
| Side effects | May run inside the transaction | Registered through `AfterCommitExecutor` |

## Migration checklist

### 1. Update the build

- Upgrade the Spring Boot parent and Boot-managed dependencies together.
- Use the Boot 4 starters and compatible MyBatis-Plus and SpringDoc versions from the reference project.
- Review Jackson 3 imports and custom modules.
- Keep test-only dependencies in `test` scope.
- Use JDK 21 for `2.0.0` or JDK 25 for `2.1.0` and later.

Do not combine the old Boot BOM with individually upgraded Boot 4 artifacts.

### 2. Replace caller and tenant handling

- Add `AuthenticatedCaller` to use-case contracts and resolve it in inbound adapters from verified authentication; do not expose it in API or facade contracts.
- Introduce a validated `TenantId` value object.
- Pass `TenantId` to every repository and cache operation.
- Apply tenant predicates unconditionally in SQL.
- Map only a verified Spring Security principal to `AuthenticatedCaller`.
- Remove the domain ThreadLocal and any null-tenant overloads.

Client-controlled headers must not supply authorities or administrator state. The `X-Dev-*` adapter remains restricted to `dev` or `test`, requires `atom.security.trusted-header.enabled=true`, and is unavailable under `prod`.

### 3. Update names and operation templates

| Older name | Current name |
|---|---|
| `api.enums.UserStatus` | `domain.model.UserStatus` |
| `EventEnum` | `UseCaseOperation` |
| `ErrorCodeEnum` | `ApplicationErrorCode` |
| `AppException` | `ApplicationException` |
| `AppUnRetryException` | `NonRetryableApplicationException` |
| `AbstractOperatorServiceTemplate` | `CommandServiceTemplate` |
| `AbstractQueryServiceTemplate` | `QueryServiceTemplate` |
| `ServiceCallback<T>` | `ServiceOperation<T>` |
| domain `CacheService` | application output port `CacheStore` |
| shared `DistributedLock` | Removed; add a use-case-specific application output port only when it has a consumer |

The operation lifecycle is `validate → prepare → execute → onSuccess`. Use command templates for state changes and query templates for reads.

### 4. Migrate the database

Before enabling Flyway on an existing database:

1. Compare the live schema with the reference `V1` migration.
2. Create a baseline and forward-migration plan; do not rerun a create-table migration against existing tables.
3. Add and backfill `tenant_id` before applying `NOT NULL`.
4. Add and backfill the optimistic-lock `version` column.
5. Resolve duplicate data before creating tenant-scoped unique indexes.
6. Remove competing `schema.sql`, Docker init SQL, and test schema sources after Flyway coverage is in place.

Do not edit a Flyway migration that has run in a shared environment. Add a new versioned migration.

Restore the persisted version during aggregate reconstitution. Include tenant and version in update conditions, and map a zero-row update to `AggregateVersionConflictException`.

### 5. Update Redis and side effects

Start with `atom.redis.enabled=false`. When Redis is enabled, include tenant ID in cache keys and verify serializers against Boot 4 and Jackson 3.

Move cache changes and in-process event publication to `AfterCommitExecutor`. Use a transactional outbox for effects that must survive process failure or cross service boundaries.

### 6. Update configuration

| Concern | Older behavior | Current behavior |
|---|---|---|
| Active profile | `dev` activated implicitly | Set `SPRING_PROFILES_ACTIVE` explicitly |
| Development identity | General `X-User-Id`/`X-Tenant-Id` headers | `X-Dev-*`, dev/test profile, explicit enable flag |
| Administrator role | Could be read from `X-Admin` | Never accepted from a request header |
| Redis | Redis host implicitly created required beans | `atom.redis.enabled`; no-op cache when false |
| Production database | Local/root fallbacks | Required environment values |
| Schema initialization | Multiple SQL files | Flyway only |
| Health details | Always visible | Visible only when authorized |

## Verify the upgrade

```bash
sh ./mvnw clean install
CI=true sh ./mvnw test
sh ./mvnw dependency:tree
```

Verify authentication (401), authorization (403), tenant isolation, tenant-scoped uniqueness, optimistic-lock conflicts, rollback behavior, Redis-disabled startup, and Flyway validation against production-like data.

Use additive database migrations where rollback compatibility is required. Remove old columns and identity contracts only after the new version is stable.
