# Configuration reference

Runtime configuration lives in the repository-level `conf/` directory. The `start` module adds this directory to its classpath during the Maven build. Do not create a second configuration tree under `start/src/main/resources`.

## Profiles

| File | Purpose | Important behavior |
| --- | --- | --- |
| `conf/application.yml` | Defaults shared by every environment | Flyway enabled, Redis disabled, trusted headers disabled, health-only actuator exposure |
| `conf/application-dev.yml` | Local MySQL and optional Redis | Trusted headers remain disabled until explicitly enabled |
| `conf/application-test.yml` | MySQL Testcontainers integration tests | Trusted headers enabled for test requests, Redis disabled |
| `conf/application-prod.yml` | Production | Datasource variables required, trusted headers forced off, API documentation disabled by default |

No profile is selected implicitly. Start the application with one profile:

```bash
sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

For a packaged application:

```bash
java -jar start/target/*-start.jar --spring.profiles.active=prod
```

Do not activate `dev` and `prod` together.

## Environment variables

| Variable | Use |
| --- | --- |
| `SPRING_DATASOURCE_URL` | Production JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Production database user |
| `SPRING_DATASOURCE_PASSWORD` | Production database password |
| `ATOM_SECURITY_TRUSTED_HEADER_ENABLED` | Enable trusted development headers in `dev` or `test` only |
| `SERVER_ADDRESS` | Override the development bind address |
| `ATOM_REDIS_ENABLED` | Enable the Redis cache adapter |
| `SPRING_DATA_REDIS_HOST` | Redis host |
| `SPRING_DATA_REDIS_PORT` | Redis port |
| `SPRING_DATA_REDIS_PASSWORD` | Redis password |
| `SPRING_DATA_REDIS_SSL_ENABLED` | Enable Redis TLS |
| `SPRINGDOC_API_DOCS_ENABLED` | Enable OpenAPI JSON |
| `SPRINGDOC_SWAGGER_UI_ENABLED` | Enable Swagger UI |

## Local database

The `dev` profile matches the MySQL service in `docker-compose.yml`:

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

## Production settings

The `prod` profile has no default datasource credentials. Supply all three datasource variables:

```bash
SPRING_DATASOURCE_URL='jdbc:mysql://db.example:3306/app' \
SPRING_DATASOURCE_USERNAME='app_runtime' \
SPRING_DATASOURCE_PASSWORD='use-a-secret-manager' \
  java -jar start/target/*-start.jar --spring.profiles.active=prod
```

Store production values in a secret manager or deployment platform. Do not commit passwords, tokens, private keys, or connection strings.

The generated `LoggingUserNotificationAdapter` is for local development and tests. It is disabled in `prod` and never reports successful delivery. Implement `application.port.out.UserNotificationPort` in `infra/external` before production startup; without a production adapter, startup fails with a missing-bean error.

OpenAPI JSON and Swagger UI are disabled in `prod`. If they are required, set both SpringDoc variables to `true` and protect the routes at the gateway or in `SecurityConfig`.

## Flyway

Flyway is enabled by default and is the only schema initialization mechanism. Migrations live under:

```text
infra/persistence/src/main/resources/db/migration/
```

| Property | Value |
| --- | --- |
| `spring.flyway.enabled` | `true` |
| `spring.flyway.locations` | `classpath:db/migration` |
| `spring.flyway.validate-on-migrate` | `true` |
| `spring.flyway.clean-disabled` | `true` |
| `spring.flyway.baseline-on-migrate` | `false` |
| `spring.sql.init.mode` | `never` |

Add a migration for every database change. Never edit a migration already applied to a shared environment. Give the production migration user only the DDL rights required by the release.

## Redis

Redis is disabled by default:

```text
atom.redis.enabled=false
```

When disabled, `NoOpCacheService` satisfies the cache port and reads continue to use MySQL.

For local Redis:

```bash
docker compose up -d redis
```

Start the application with `ATOM_REDIS_ENABLED=true`, then use the standard Spring Data Redis variables for a remote server. Cache keys include tenant identity. Cache failure must not bypass authorization or database ownership checks.

## HTTP security

The default route policy is fail-closed:

- health is public;
- OpenAPI discovery is public only while its endpoints are enabled;
- user reads require `users:read`;
- user creation and updates require `users:write`;
- user deletion requires `users:delete`;
- unmatched API requests require authentication;
- all other unmatched routes are denied.

Application services repeat the authority check and derive `TenantId` from `AuthenticatedCaller`, so facade and non-HTTP adapters follow the same policy.

### Trusted development headers

The development adapter accepts `X-Dev-User-Id` and `X-Dev-Tenant-Id`. Both values must be positive integers; authorities come from server configuration, not request headers.

Enable it locally:

```bash
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

The adapter is disabled by default, requires `dev` or `test`, and refuses `prod`. The `dev` profile binds to `127.0.0.1` unless `SERVER_ADDRESS` is deliberately overridden. Never forward these headers from an internet-facing proxy.

### Production authentication

Production must verify a real credential, including its signature, issuer, audience, expiry, and required claims. Map the verified subject and tenant claim to the infrastructure principal, map verified scopes or roles to the three user authorities, and let `AuthenticatedCallerMapper` create the API context. Never construct `AuthenticatedCaller` from request JSON or arbitrary client headers.

### Password hashing

`infra/security` supplies a replaceable BCrypt adapter. The `atom.security.password.bcrypt-strength` property defaults to `12`. The domain receives only a `PasswordHash`; plaintext is not stored in the aggregate or persistence object.

## MyBatis-Plus

Mapper XML is loaded from `classpath*:mapper/**/*.xml`. Underscore-to-camel-case mapping is enabled and second-level cache is disabled. Queries and updates include `tenant_id`; `version` is checked by the optimistic-lock interceptor; pagination is the last MyBatis-Plus inner interceptor. The project does not use MyBatis logical delete.
