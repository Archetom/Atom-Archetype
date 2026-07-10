# Upgrade guide

A Maven archetype creates source code once; it is not an in-place application updater. Changing the Atom Archetype version does not rewrite an existing generated project. Upgrade by generating a reference project, reviewing the delta, and applying changes intentionally to your codebase.

This guide targets the breaking `2.0.0` architecture. Maven Central `1.1.0` is the legacy Spring Boot 3.5 release. Pin the exact `2.0.0` release when generating a migration reference project.

## Safe upgrade workflow

1. Commit or otherwise back up the application being upgraded.
2. Read the release notes and this guide before changing runtime dependencies.
3. Generate a reference project with the same group/package conventions but a different artifact ID.
4. Compare parent/module POMs, configuration, migrations, architecture contracts, and tests—not the sample business data alone.
5. Apply database migrations with a copy of production-like data.
6. Run unit, integration, tenant-isolation, authorization, and concurrency tests.
7. Deploy in a reversible sequence and observe authentication failures, Flyway state, optimistic-lock conflicts, and post-commit delivery failures.

Generate the 2.0 reference project:

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.0.0 \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service-reference \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

## Moving from the 1.1.0 legacy architecture

The exact delta depends on when the original project was generated. Review each area below rather than applying a blind directory copy.

### Java and Spring Boot 4

- Use JDK 21 for compilation and runtime.
- Upgrade to the Spring Boot 4 parent and Boot-managed dependency set as one change.
- Replace broad/legacy starters with the Boot 4 modules used by the reference project, such as `spring-boot-starter-webmvc`, `spring-boot-starter-webclient`, `spring-boot-starter-jackson`, and `spring-boot-starter-flyway`.
- Use the Boot 4 MyBatis-Plus starter and compatible SpringDoc version.
- Review Jackson 3 imports and custom modules; Boot 4 code can use `tools.jackson.*` rather than Jackson 2's `com.fasterxml.jackson.*` APIs.
- Keep test-only dependencies in `test` scope and update Testcontainers artifact names to the current modules.

Do not mix the old Boot BOM with a few manually upgraded Boot 4 starters. First make dependency management coherent, then resolve source-level changes.

### Explicit authenticated caller and tenant

Older templates populated a domain `UserContextHolder` from client-provided `X-User-Id`, `X-Tenant-Id`, and `X-Admin` headers. The current design removes that ThreadLocal boundary.

- Add `AuthenticatedCaller` to API/use-case contracts.
- Introduce a validated `TenantId` value object.
- Pass tenant scope explicitly to repositories and caches.
- Make every SQL tenant predicate unconditional.
- Map only a verified Spring Security principal into `AuthenticatedCaller`.
- Never accept roles or administrator state from client-controlled headers.

For local development, the replacement headers are `X-Dev-User-Id` and `X-Dev-Tenant-Id`; they work only in `dev`/`test` with `atom.security.trusted-header.enabled=true`.

This is an intentional source compatibility break. Do not keep a null-tenant overload as a compatibility shortcut.

### Security configuration

The current HTTP boundary is fail-closed:

- business APIs require authentication and authorities;
- trusted-header authentication is disabled by default and unavailable under `prod`;
- production database credentials have no fallback values;
- health is anonymous; OpenAPI endpoints are anonymous only when enabled and are disabled by the production profile by default.

Before production rollout, configure the selected IdP and verify 401 (missing/invalid credential) separately from 403 (authenticated but insufficient authority). If the application uses cookie/session authentication instead of a stateless bearer or mTLS model, revisit CSRF rather than copying the stateless API setting unchanged.

### Domain and application naming

Use the current names consistently:

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
| shared `DistributedLock` | application output port `DistributedLock` |

The current `ServiceOperation` lifecycle is `validate → prepare → execute → onSuccess`. Do not re-create persistence and concurrency phases as empty ceremonial hooks.

### Flyway and database schema

The current template treats Flyway migrations in `infra/persistence/src/main/resources/db/migration` as the only schema source.

Before enabling Flyway on an existing production database:

1. compare the live schema with the reference `V1` migration;
2. create an appropriate baseline/migration plan rather than rerunning a create-table migration;
3. add and backfill `tenant_id` before making it `NOT NULL`;
4. add/backfill the optimistic-lock `version` column;
5. create tenant-scoped unique indexes only after duplicate data is resolved;
6. remove competing `schema.sql`, Docker init SQL, or test schemas after migration coverage exists.

Never edit a Flyway migration that has already run in a shared environment. Add a new versioned migration.

### Optimistic locking and deletion

- Restore persistence version during aggregate reconstitution.
- Include tenant and version in update conditions.
- Treat an update count of zero as `AggregateVersionConflictException` and map it to an appropriate conflict response.
- Reload and retry only if the whole use case is safe to repeat.
- Choose one deletion model. The reference uses domain status and deliberately omits a second `deleted_time` mechanism.

### Cache, Redis, and locks

Set `atom.redis.enabled=false` initially. The no-op cache must preserve behavior while the Redis adapter is disabled.

When enabling Redis:

- migrate cache keys to include tenant ID;
- accept a cold-cache deployment or expire old global keys;
- verify serializers against Boot 4/Jackson 3;
- keep database uniqueness and optimistic locking as the consistency authority;
- use an owner-specific `LockHandle` if distributed coordination is genuinely required.

Do not make application startup depend on an optional Redis bean.

### Transactions and events

Move cache writes/evictions and in-process event publication to `AfterCommitExecutor`. This prevents external effects for a transaction that rolls back.

After-commit callbacks are still not durable. Introduce a transactional outbox before relying on the event path for cross-service delivery, billing, email guarantees, or other effects that cannot be lost.

## Configuration changes

| Concern | Older behavior | Current behavior |
|---|---|---|
| Active profile | `dev` activated implicitly | Set `SPRING_PROFILES_ACTIVE` explicitly |
| Development identity | General `X-User-Id`/`X-Tenant-Id` headers | `X-Dev-*`, dev/test profile plus explicit enable flag |
| Administrator role | Could be read from `X-Admin` | Never accepted from a request header |
| Redis | Presence of Redis host implicitly created required beans | `atom.redis.enabled`, no-op cache when false |
| Production database | Local/root fallbacks | Required environment values, fail-fast |
| Schema initialization | Multiple SQL files | Flyway only |
| Health details | Always visible | Visible only when authorized |

Review secrets management separately; configuration placeholders are not a secret store.

## Verification checklist

```bash
sh ./mvnw clean install
CI=true sh ./mvnw test
sh ./mvnw dependency:tree
```

Also verify:

- anonymous business request → 401;
- valid identity without authority → 403;
- tenant A cannot read or modify tenant B;
- duplicate username/email is scoped by tenant;
- stale version update is rejected;
- failed transaction publishes no event and writes no cache;
- application starts with Redis disabled;
- Flyway validates the production-like schema.

## Rollback considerations

Source rollback is straightforward; database and identity-contract rollback may not be. Prefer backward-compatible additive migrations, deploy schema before code when required, and do not remove old columns until the new version has been stable. Cache keys can usually be abandoned safely, but authentication/header compatibility should have an explicit cutover window at trusted boundaries—not a permanent insecure fallback.
