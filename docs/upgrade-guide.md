# Upgrade guide

A Maven archetype generates source code once. It does not update an existing project. Generate a reference project from the target revision, compare it with the application, and apply the required changes.

## Available versions

| Source | Status | Java | Notes |
|---|---|---|---|
| Maven Central `1.1.0` | Published release | Legacy baseline | Spring Boot 3.5 architecture |
| Git tag `v2.0.0` | Release tag; not published to Maven Central | 21 | First release of the 2.x architecture |
| `main` / `2.1.0-SNAPSHOT` | Current development line | 25 | Spring Boot 4.1 and the latest template changes |

Install the selected revision locally before generating a reference project. For the `v2.0.0` tag:

```bash
./mvnw clean install -Dgpg.skip=true

cd ..
./Atom-Archetype/mvnw -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.0.0 \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service-reference \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

## Major changes from `1.1.0`

| Area | `1.1.0` | 2.x architecture |
|---|---|---|
| Runtime | Spring Boot 3.5 | Spring Boot 4; JDK 21 on `v2.0.0`, JDK 25 on `main` |
| Caller context | Domain `UserContextHolder` | Explicit API `AuthenticatedCaller` |
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
- Use JDK 21 for the `v2.0.0` tag or JDK 25 for current `main`.

Do not combine the old Boot BOM with individually upgraded Boot 4 artifacts.

### 2. Replace caller and tenant handling

- Add `AuthenticatedCaller` to API and use-case contracts.
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
