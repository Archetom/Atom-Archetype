#set( $h2 = '##' )
# ${rootArtifactId}

[简体中文](README.zh-CN.md)

A production-oriented Java 21 and Spring Boot 4.1 multi-module starter for Domain-Driven Design (DDD), hexagonal boundaries, explicit multi-tenancy, MyBatis-Plus, MySQL, Flyway, and optional Redis caching.

Generated coordinates: `${groupId}:${rootArtifactId}:${version}`.

This generated project contains a working user aggregate as an executable example. Replace or extend the example while preserving the dependency and security rules documented below.

$h2 Key properties

- Domain logic is independent of HTTP, MyBatis, Redis, and other infrastructure.
- Every user repository and cache operation requires an explicit `TenantId`.
- Authentication becomes an explicit `AuthenticatedCaller` at the API boundary; identity is never read from a request body.
- Commands and queries use separate `CommandServiceTemplate` and `QueryServiceTemplate` policies.
- Database changes are append-only Flyway migrations.
- Optimistic locking protects aggregate updates through a persisted `version` field.
- Cache updates and domain event publication run only after a successful transaction commit.
- Redis is optional. With Redis disabled, a no-op adapter keeps business behavior correct.
- Production rejects anonymous API access and cannot enable the development trusted-header adapter.

$h2 Requirements

- JDK 21
- Docker with Docker Compose for local MySQL and integration tests
- MySQL 8.4.10 or a compatible MySQL 8 server
- Make (optional; every Make target has an equivalent `sh ./mvnw` command)

Redis is optional.

$h2 Quick start

Start MySQL:

```bash
docker compose up -d mysql
```

Build the complete reactor:

```bash
sh ./mvnw clean install
```

Run the application with the development profile and explicitly enable local trusted headers:

```bash
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway creates the schema automatically. Configuration is loaded from `conf/`, not from `start/src/main/resources`.

Check the public health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Call an authenticated development endpoint:

```bash
curl \
  -H 'X-Dev-User-Id: 1' \
  -H 'X-Dev-Tenant-Id: 1' \
  http://localhost:8080/api/v1/users
```

In development, API documentation is available at `http://localhost:8080/swagger-ui/index.html` and `/v3/api-docs`. Production disables both endpoints by default; opt in only after applying an appropriate access policy.

If trusted headers are not explicitly enabled, application endpoints correctly return HTTP 401.

$h2 Optional Redis cache

Start Redis and enable its adapter:

```bash
docker compose up -d redis

ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
ATOM_REDIS_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Use the standard Spring Boot Redis environment variables to change host, port, credentials, or TLS settings. A Redis failure must never be treated as the source of business truth.

$h2 Common commands

```bash
make compile           # compile every module
make test              # fast tests; Docker integration tests stay disabled
make integration-test  # full suite with MySQL Testcontainers
make infra-up          # start local MySQL and Redis
make run               # run the start module with the dev profile
make clean-sample      # remove the bundled User example safely
```

`make clean-sample` calls `bash clean.sh` because Maven archetypes do not preserve executable bits portably.

$h2 Modules

| Module | Responsibility |
| --- | --- |
| `api` | Public requests, responses, facade contracts, and `AuthenticatedCaller` |
| `domain` | Aggregates, value objects, domain services, events, and repository ports |
| `application` | Use-case orchestration, command/query policies, output ports, and transaction callbacks |
| `shared` | Small cross-cutting result and error primitives; no business model |
| `infra/rest` | Spring MVC controllers, Spring Security, and HTTP error mapping |
| `infra/persistence` | MyBatis-Plus adapters, Flyway migrations, optimistic locking, and cache adapters |
| `infra/external` | Third-party adapters implementing application output ports |
| `infra/security` | Password hashing and security technology adapters |
| `infra/facade` | Implementation of public API facade contracts |
| `start` | Spring Boot composition root and integration tests |

The central dependency direction is:

```text
HTTP / persistence / external adapters
                  ↓
             application
                  ↓
               domain
```

`domain` must never depend on `application`, `api`, or `infra`.

$h2 Security model

All application use cases accept an `AuthenticatedCaller`. The application derives a validated `TenantId` from that caller and passes it explicitly to repositories and caches.

The bundled `X-Dev-User-Id` and `X-Dev-Tenant-Id` authentication adapter is only a local development and test convenience. It is installed only when all of these conditions are true:

- the active profile is `dev` or `test`;
- the active profile is not `prod`;
- `atom.security.trusted-header.enabled` is explicitly `true`.

Never expose that adapter through a public gateway. Production must supply a real authentication adapter such as an OAuth2 resource server and map its verified principal to `AuthenticatedCaller`.

The included `LoggingUserNotificationAdapter` is also non-production only. Implement `UserNotificationPort` with a real provider before starting with the `prod` profile; the missing bean is designed to fail fast.

See [configuration](docs/configuration.md) for the complete profile and secret policy.

$h2 Database and deletion semantics

- Flyway migrations live in `infra/persistence/src/main/resources/db/migration`.
- Never edit a migration that has already been applied; add a new versioned migration.
- `UserStatus.DELETED` is the single soft-delete representation. There is no hidden MyBatis logical-delete column.
- Tenant-scoped unique keys reserve usernames and email addresses inside each tenant.
- A stale aggregate version produces an optimistic-lock conflict instead of overwriting a newer update.

$h2 Tests

Run fast unit and module tests:

```bash
sh ./mvnw test
```

Run Docker-backed MySQL integration tests:

```bash
CI=true sh ./mvnw test
```

The integration suite verifies Flyway, tenant isolation, full PO mapping, optimistic locking, soft deletion, HTTP authentication, and error status mapping.

The supported baseline is a JVM deployment. GraalVM native-image support is not generated because it is not part of the verified test matrix.

$h2 Documentation

- [Architecture and invariants](docs/architecture.md)
- [HTTP API reference](docs/api-reference.md)
- [Development workflow](docs/usage-guide.md)
- [Configuration and security](docs/configuration.md)
- [Object and naming conventions](docs/object-layering.md)
- [Testing guide](docs/test-guide.md)
- [AI contributor instructions](AGENTS.md)
- [LLM documentation index](llms.txt)

$h2 License

MIT. See [LICENSE](LICENSE).
