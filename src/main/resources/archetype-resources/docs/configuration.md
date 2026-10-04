# Configuration reference

Runtime configuration lives in the repository-level `conf/` directory. The `start` module adds this directory to its classpath during the Maven build. Do not create a second configuration tree under `start/src/main/resources`.

Test-only configuration lives in `start/src/test/resources`. It is on the test classpath only and is never packaged into the application jar.

## Profiles

| File | Purpose | Important behavior |
| --- | --- | --- |
| `conf/application.yml` | Defaults shared by every environment | Flyway enabled, Redis disabled, trusted headers disabled, health-only actuator exposure |
| `conf/application-dev.yml` | Local MySQL and optional Redis | Trusted headers remain disabled until explicitly enabled |
| `start/src/test/resources/application-test.yml` | MySQL Testcontainers integration tests | Trusted headers enabled for test requests, Redis disabled; test resource only |
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

The generated Compose file pins MySQL 26.7.0. Before reusing a data volume created by an older MySQL release, back it up and follow MySQL's supported upgrade path; for disposable development data, create a fresh volume instead.

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

### Tenant identity and selection

Tenant identity is established by the verified authentication principal and is passed explicitly through every use case. A request body, query parameter, or untrusted header cannot select a tenant. The dev/test trusted-header adapter described below is the sole exception: it accepts both headers only when explicitly enabled and remains unavailable in `prod`. Tenant membership discovery and switching require the future organization module and remain unavailable until it can verify the caller's membership.

### Trusted development headers

The development adapter accepts `X-Dev-User-Id` and `X-Dev-Tenant-Id`. Both values must be positive integers; authorities come from server configuration, not request headers.

Enable it locally:

```bash
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

The adapter is disabled by default, requires `dev` or `test`, and refuses `prod`. The `dev` profile binds to `127.0.0.1` unless `SERVER_ADDRESS` is deliberately overridden. The `test` profile that enables the adapter is a test resource, so activating `test` on a packaged application does not enable it. Never forward these headers from an internet-facing proxy.

### Production authentication

Production must verify a real credential, including its signature, issuer, audience, expiry, and required claims. Map the verified subject and tenant claim to an `ActorPrincipal`, map verified scopes or roles to the three user authorities, and put the resulting authentication into Spring Security's context; `AuthenticatedCallerResolver` turns it into the use-case caller. Never construct `AuthenticatedCaller` from request JSON, RPC arguments, or arbitrary client headers.

### RPC exposure

`UserFacade` is the contract published to RPC clients. Its methods carry no identity, tenant, or authority parameters; `UserFacadeImpl` resolves the caller from Spring Security's context through `AuthenticatedCallerResolver`. The RPC server adapter, such as a framework interceptor or filter, must therefore:

1. Verify a real credential carried in the RPC metadata, including its signature, issuer, audience, expiry, and tenant claim. Never trust actor, tenant, or authority values sent as plain metadata or method arguments.
2. Put the verified `ActorPrincipal` and authorities into a new security context on the thread that invokes the facade.
3. Clear the context when the call ends, because RPC frameworks reuse pooled threads.

```java
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
        new ActorPrincipal(verifiedUserId, verifiedTenantId), null, verifiedAuthorities));
SecurityContextHolder.setContext(context);
try {
    return invocation.proceed();
} finally {
    SecurityContextHolder.clearContext();
}
```

A call without this context reaches the use case without a caller and returns `AUTHENTICATION_REQUIRED` in its `Result`. If the framework invokes the facade on a different thread than its interceptor, establish the context on that thread. Use cases still check authorities through `CallerGuard`, so RPC callers need the same `users:*` authorities as HTTP callers.

### Password hashing

`infra/security` supplies a replaceable BCrypt adapter. The `atom.security.password.bcrypt-strength` property defaults to `12`. The domain receives only a `PasswordHash`; plaintext is not stored in the aggregate or persistence object.

## Logging and request correlation

`conf/logback-spring.xml` writes the thread, request ID, and logger name on every line.

- Every HTTP response, including authentication failures and errors, carries an `X-Request-Id` header. A well-formed incoming value from a gateway (letters, digits, `.`, `_`, or `-`, at most 64 characters) is reused; any other value is replaced so callers cannot forge log lines. The ID follows async event listeners.
- Expected rejections, such as validation, authorization, and domain-rule failures, log one WARN line without a stack trace.
- Unexpected failures log the full exception at ERROR, including its message, causes, and stack trace. Public error responses still never expose these details.
- Logs are therefore sensitive. Driver and client messages can contain personal data, such as the duplicate value in a unique-key error. Restrict log access and retention, and never put credentials or secrets into your own exception or log messages.
- Malformed request bodies are logged without parser details, because those messages can echo request content such as passwords.
- Spring MVC client errors, such as an unknown route (404) or an unsupported method (405), keep their status and are not reported as internal failures.

## MyBatis-Plus

Mapper XML is loaded from `classpath*:mapper/**/*.xml`. Underscore-to-camel-case mapping is enabled and second-level cache is disabled. Queries and updates include `tenant_id`; `version` is checked by the optimistic-lock interceptor; pagination is the last MyBatis-Plus inner interceptor. The project does not use MyBatis logical delete.
