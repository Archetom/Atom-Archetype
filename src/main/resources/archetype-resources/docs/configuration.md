# Configuration and security

Runtime configuration lives in the repository-level `conf/` directory. The `start` module adds that directory to its classpath during the Maven build.

Do not create a second authoritative configuration tree under `start/src/main/resources`.

## Profiles

| File | Purpose | Important behavior |
| --- | --- | --- |
| `conf/application.yml` | Safe defaults shared by every environment | Flyway enabled, Redis disabled, trusted headers disabled, health-only actuator exposure |
| `conf/application-dev.yml` | Local MySQL and optional Redis | Trusted headers remain disabled until explicitly enabled |
| `conf/application-test.yml` | MySQL Testcontainers integration tests | Trusted-header authentication enabled for test requests, Redis disabled |
| `conf/application-prod.yml` | Production | Requires datasource environment variables, forces trusted headers off, and disables API documentation by default |

No runtime profile is selected implicitly. Activate one deliberately:

```bash
sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

For a packaged application:

```bash
java -jar start/target/*-start.jar --spring.profiles.active=prod
```

Do not activate `dev` and `prod` together.

## Production notification adapter

The generated `LoggingUserNotificationAdapter` records notification intent for local development and tests. It is disabled whenever the `prod` profile is active and never claims that delivery succeeded.

Before starting in production, implement `application.port.out.UserNotificationPort` in `infra/external` using your selected provider. If no production adapter exists, application startup fails fast with a missing-bean error.

## Local database

The development profile matches the MySQL service in `docker-compose.yml`:

| Setting | Development value |
| --- | --- |
| Host | `localhost:3306` |
| Database | `atom_db` |
| Username | `atom_user` |
| Password | `atom_pass` |

Start it with:

```bash
docker compose up -d mysql
```

These credentials are local examples only.

## Production datasource

The production profile has no default credentials. Supply at least:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Example:

```bash
SPRING_DATASOURCE_URL='jdbc:mysql://db.example:3306/app' \
SPRING_DATASOURCE_USERNAME='app_runtime' \
SPRING_DATASOURCE_PASSWORD='use-a-secret-manager' \
  java -jar start/target/*-start.jar --spring.profiles.active=prod
```

Store production values in a secret manager or deployment platform. Do not commit real passwords, tokens, private keys, or connection strings.

## Flyway

Flyway is enabled by default and is the only schema initialization mechanism.

```text
infra/persistence/src/main/resources/db/migration/
└── V1__create_user_table.sql
```

Shared defaults:

| Property | Value | Reason |
| --- | --- | --- |
| `spring.flyway.enabled` | `true` | Migrate before persistence is used |
| `spring.flyway.locations` | `classpath:db/migration` | One discoverable migration source |
| `spring.flyway.validate-on-migrate` | `true` | Detect checksum and history drift |
| `spring.flyway.clean-disabled` | `true` | Prevent accidental schema deletion |
| `spring.flyway.baseline-on-migrate` | `false` | Do not silently accept unmanaged schemas |
| `spring.sql.init.mode` | `never` | Prevent competing `schema.sql` initialization |

Add a new migration for every database change. Never edit a migration already applied to a shared environment. Production migration users should have only the DDL rights actually required by the release process.

## Authentication and authorization

HTTP security is fail-closed:

- health is public, and OpenAPI discovery is public only when its endpoints are enabled;
- user API reads require `users:read`;
- user creation and updates require `users:write`;
- user deletion requires `users:delete`;
- unmatched API requests require authentication;
- all other unmatched routes are denied.

Application services repeat the authority check and derive `TenantId` from `AuthenticatedCaller`. This protects non-HTTP callers such as facade or message adapters.

### Development trusted headers

The bundled adapter recognizes:

- `X-Dev-User-Id`
- `X-Dev-Tenant-Id`

Both values must be positive integers. Authorities are server configuration, not request headers.

Enable the adapter locally:

```bash
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

The adapter requires a `dev` or `test` profile, refuses the `prod` profile, and is disabled by default. The development profile also binds the application to `127.0.0.1` unless `SERVER_ADDRESS` is deliberately overridden. Treat these headers as development credentials and never forward them from an internet-facing proxy.

### Production authentication

Replace the development adapter with a real mechanism such as Spring Security OAuth2 Resource Server:

1. verify the token signature, issuer, audience, expiry, and required claims;
2. map the verified subject and tenant claim to the infrastructure principal;
3. map verified scopes or roles to `users:read`, `users:write`, and `users:delete`;
4. let `AuthenticatedCallerMapper` create the API context;
5. add negative tests for missing tenant, invalid token, and cross-tenant access.

Never construct `AuthenticatedCaller` from request JSON or arbitrary client headers.

### Password hashing

`infra/security` supplies a replaceable BCrypt adapter with work factor `atom.security.password.bcrypt-strength` (default `12`). The domain accepts only a `PasswordHash` after the adapter runs; plaintext is never stored in the aggregate or persistence object. Benchmark work-factor changes in the deployment environment, and replace the adapter with Argon2id when the chosen runtime and maintained provider are part of your compatibility test matrix.

### Production API documentation

The production profile disables OpenAPI JSON and Swagger UI by default. If an operational need justifies exposing them, set `SPRINGDOC_API_DOCS_ENABLED=true` and `SPRINGDOC_SWAGGER_UI_ENABLED=true`, then protect the routes at the gateway or adjust `SecurityConfig` for authenticated access.

## Optional Redis

Redis is disabled by default:

```text
atom.redis.enabled=false
```

When disabled, `NoOpCacheService` satisfies the cache port. The application remains correct and queries MySQL.

For local Redis:

```bash
docker compose up -d redis
```

Then start with `ATOM_REDIS_ENABLED=true`. Configure remote Redis through standard Spring Boot variables such as:

- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATA_REDIS_PASSWORD`
- `SPRING_DATA_REDIS_SSL_ENABLED`

Cache keys must include tenant identity. Cache failures should be observed and degraded safely, never used to bypass authorization or database ownership checks.

## Locale and time defaults

The generated baseline stores and serializes timestamps in UTC. Convert them to a user's locale only at an explicit presentation boundary. Phone numbers use E.164 form (`+` followed by country code and subscriber number) so the domain model does not silently assume one country.

## MyBatis-Plus

Mapper XML is loaded from `classpath*:mapper/**/*.xml`. Underscore-to-camel-case mapping is enabled and second-level cache is disabled.

Persistence invariants:

- queries and updates include `tenant_id`;
- `version` is mapped and checked by the optimistic-lock interceptor;
- pagination is the last MyBatis-Plus inner interceptor;
- no MyBatis logical-delete configuration is used;
- SQL column changes are synchronized with PO, converter, mapper XML, and aggregate reconstruction.

## Task executor

The `task.executor` namespace configures core threads, maximum threads, queue capacity, keep-alive time, and thread name prefix. Tune these values from measured workload and define rejection behavior before using the executor for critical work.

Caller and tenant context are explicit arguments; do not reintroduce ThreadLocal propagation for async tasks.

## Observability

The default actuator exposure is health only. Health details are shown only to authorized callers. Prometheus export is disabled until a deployment explicitly enables and exposes it.

Before production, decide explicitly:

- which actuator endpoints are exposed;
- authentication for management endpoints;
- metric cardinality limits;
- log retention and redaction;
- whether Prometheus export is enabled.

## Configuration checklist

- Is exactly one intended profile active?
- Are datasource credentials supplied outside Git?
- Is trusted-header authentication disabled outside local development and tests?
- Does the production authentication adapter supply a verified actor and tenant?
- Is Redis explicitly enabled only when configured?
- Are Flyway validation and clean protection still enabled?
- Are management endpoints and logging appropriate for the environment?
