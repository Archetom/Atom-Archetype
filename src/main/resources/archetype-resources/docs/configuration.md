# Configuration reference

Runtime configuration lives in the repository-level `conf/` directory. The `start` module adds this directory to its classpath during the Maven build. Do not create a second configuration tree under `start/src/main/resources`.

Test-only configuration lives in `start/src/test/resources`. It is on the test classpath only and is never packaged into the application jar.

## Profiles

| File | Purpose | Important behavior |
| --- | --- | --- |
| `conf/application.yml` | Defaults shared by every environment, including MyBatis-Plus, logging, and the async executor | Flyway enabled, Redis disabled, trusted headers disabled, health-only actuator exposure |
| `conf/application-dev.yml` | Local MySQL and optional Redis | Trusted headers remain disabled until explicitly enabled |
| `start/src/test/resources/application-test.yml` | MySQL Testcontainers integration tests | Trusted headers enabled for test requests, Redis disabled; test resource only |
| `conf/application-prod.yml` | Production | Datasource variables required, trusted headers forced off, API documentation disabled by default |

Profile files hold only what differs by environment. Put a shared setting in `application.yml`, so tests exercise the same value as production.

No profile is selected implicitly. Start the application with one profile:

```bash
sh ./mvnw -pl start -am spring-boot:run -Dspring-boot.run.profiles=dev
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
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | Trusted JWT issuer for HTTP and gRPC bearer tokens |
| `ATOM_GRPC_ENABLED` | Enable the gRPC server; also requires a JWT decoder, see [RPC exposure](#rpc-exposure) |
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

The generated Compose file pins the MySQL image, and the integration tests read the same image from it. Before reusing a data volume created by an older MySQL release, back it up and follow MySQL's supported upgrade path; for disposable development data, create a fresh volume instead.

## Production settings

The `prod` profile has no default datasource credentials. Supply all three datasource variables:

```bash
SPRING_DATASOURCE_URL='jdbc:mysql://db.example:3306/app' \
SPRING_DATASOURCE_USERNAME='app_runtime' \
SPRING_DATASOURCE_PASSWORD='use-a-secret-manager' \
  java -jar start/target/*-start.jar --spring.profiles.active=prod
```

Store production values in a secret manager or deployment platform. Do not commit passwords, tokens, private keys, or connection strings.

Configure the trusted JWT issuer as well, for example `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`; otherwise every API request is rejected with HTTP 401. See [Production authentication](#production-authentication).

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
- routes declared by an `ApiRouteAuthorization` bean require its authorities; the sample `UserRouteAuthorization` requires `users:read` for reads, `users:write` for creation and updates, and `users:delete` for deletion;
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
  sh ./mvnw -pl start -am spring-boot:run -Dspring-boot.run.profiles=dev
```

The adapter is disabled by default, requires `dev` or `test`, and refuses `prod`. The `dev` profile binds to `127.0.0.1` unless `SERVER_ADDRESS` is deliberately overridden. The `test` profile that enables the adapter is a test resource, so activating `test` on a packaged application does not enable it. Never forward these headers from an internet-facing proxy.

### Production authentication

HTTP and gRPC accept bearer JWTs whenever Spring Boot can build a JWT decoder. Configure the trusted issuer with one of the standard properties:

```bash
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI='https://id.example.com/realms/app' \
  java -jar start/target/*-start.jar --spring.profiles.active=prod
```

`spring.security.oauth2.resourceserver.jwt.jwk-set-uri` or `public-key-location` work as well; add `audiences` when your issuer serves several applications. The decoder verifies the signature, issuer, and expiry. `ActorJwtAuthenticationConverter` then maps the claims to an `ActorPrincipal`, and `AuthenticatedCallerResolver` turns it into the use-case caller:

| Property | Default | Use |
| --- | --- | --- |
| `atom.security.jwt.user-id-claim` | `sub` | Claim holding the positive user ID |
| `atom.security.jwt.tenant-claim` | `tenant_id` | Claim holding the positive tenant ID |
| `atom.security.jwt.authorities-claim` | `scope` | Authorities as a space-separated string or a list, used verbatim, for example `users:read` |

A token whose user or tenant claim is not a positive ID is rejected. Missing or invalid tokens receive HTTP 401 with a `WWW-Authenticate: Bearer` header; `ApiRouteAuthorization` route rules and the use cases check the token's authorities. Without a decoder and without the dev/test trusted-header adapter, startup logs a warning and every API request returns HTTP 401. Never construct `AuthenticatedCaller` from request JSON, RPC arguments, or arbitrary client headers.

### RPC exposure

The opt-in `infra/grpc` module serves the facades over gRPC. Facade methods carry no identity, tenant, or authority parameters: each call is authenticated with a bearer JWT, and `UserFacadeImpl` resolves the caller from Spring Security's context through `AuthenticatedCallerResolver`.

gRPC is disabled by default. It uses the same JWT decoder and claim mapping as HTTP (see [Production authentication](#production-authentication)); enabling it without a decoder fails at startup instead of serving unauthenticated calls:

```bash
ATOM_GRPC_ENABLED=true \
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI='https://id.example.com/realms/app' \
  java -jar start/target/*-start.jar --spring.profiles.active=prod
```

| Property | Default | Use |
| --- | --- | --- |
| `atom.grpc.enabled` | `false` | Starts the gRPC server through `spring.grpc.server.enabled` |
| `spring.grpc.server.port` | `9090` | gRPC listen port |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri`, `jwk-set-uri`, or `public-key-location` | none | Verifies token signature, issuer, and expiry for HTTP and gRPC |

- Every method requires a valid token except the standard `grpc.health.v1.Health` service, which stays public for load-balancer probes. Server reflection also requires a token.
- Missing or invalid tokens, including tokens whose user or tenant claim is not a positive ID, are rejected with `UNAUTHENTICATED`.
- Use cases still check authorities through `CallerGuard`, so gRPC callers need the same `users:*` authorities as HTTP callers.
- Request messages are validated against the same API constraints as HTTP requests.

Failures keep the safe public message as the status description and the stable public error code as the `reason` of a `google.rpc.ErrorInfo` detail:

| Public error | gRPC status |
| --- | --- |
| `PARAMETER_INVALID` | `INVALID_ARGUMENT` |
| `AUTHENTICATION_REQUIRED` | `UNAUTHENTICATED` |
| `ACCESS_DENIED` | `PERMISSION_DENIED` |
| `RESOURCE_NOT_FOUND` | `NOT_FOUND` |
| `RESOURCE_ALREADY_EXISTS` | `ALREADY_EXISTS` |
| `VERSION_CONFLICT`, `CONCURRENT_OPERATION` | `ABORTED` |
| `DOMAIN_RULE_VIOLATION`, `OPERATION_NOT_ALLOWED` | `FAILED_PRECONDITION` |
| `UNKNOWN`, `SYSTEM` | `INTERNAL` |

Contracts live in `infra/grpc/src/main/proto`. The build generates the Java stubs and embeds the `.proto` files in the module jar, so clients in any language can generate their own stubs.

Any other RPC transport must follow the same rule: verify a real credential, put the verified `ActorPrincipal` and its authorities into Spring Security's context on the thread that invokes the facade, and clear the context when the call ends.

### Password hashing

`infra/security` supplies a replaceable BCrypt adapter. The `atom.security.password.bcrypt-strength` property defaults to `12`. The domain receives only a `PasswordHash`; plaintext is not stored in the aggregate or persistence object.

## Logging and request correlation

`conf/logback-spring.xml` writes the thread, request ID, and logger name on every line, to the console and to rolling files under `logging.file.path` (default `./logs/app`).

- Every HTTP response, including authentication failures and errors, carries an `X-Request-Id` header. A well-formed incoming value from a gateway (letters, digits, `.`, `_`, or `-`, at most 64 characters) is reused; any other value is replaced so callers cannot forge log lines. The ID follows async event listeners.
- Expected rejections, such as validation, authorization, and domain-rule failures, log one WARN line without a stack trace.
- Unexpected failures log the full exception at ERROR, including its message, causes, and stack trace. Public error responses still never expose these details.
- Logs are therefore sensitive. Driver and client messages can contain personal data, such as the duplicate value in a unique-key error. Restrict log access and retention, and never put credentials or secrets into your own exception or log messages.
- Malformed request bodies are logged without parser details, because those messages can echo request content such as passwords.
- Spring MVC client errors, such as an unknown route (404) or an unsupported method (405), keep their status and are not reported as internal failures.

## Async execution

`@Async` work, such as application event listeners, runs on Spring Boot's application task executor. Size it with `spring.task.execution.*` in `conf/application.yml`: 5 core threads, 10 maximum threads, and a queue of 100 tasks by default. During graceful shutdown the executor waits up to 30 seconds for queued work. `TaskConfig` registers a `TaskDecorator` that carries the request ID into each task. Work that must survive a process crash needs a transactional outbox, not this executor.

## MyBatis-Plus

Mapper XML is loaded from `classpath*:mapper/**/*.xml`. Underscore-to-camel-case mapping is enabled and second-level cache is disabled. Queries and updates include `tenant_id`; `version` is checked by the optimistic-lock interceptor; pagination is the last MyBatis-Plus inner interceptor. The project does not use MyBatis logical delete.
