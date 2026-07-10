# Atom Archetype — DDD Maven Archetype for Spring Boot 4 and Java 21

[![Maven Central legacy release](https://img.shields.io/maven-central/v/io.github.archetom/atom-archetype.svg?label=Maven%20Central%20legacy)](https://central.sonatype.com/artifact/io.github.archetom/atom-archetype)
[![CI](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml/badge.svg)](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-007396.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

English | [简体中文](README.zh-CN.md)

Atom Archetype is a Maven archetype for generating production-oriented, multi-module Java applications with Domain-Driven Design (DDD), Spring Boot 4, and Java 21. It provides explicit dependency boundaries, tenant-aware application contracts, MyBatis-Plus persistence, Flyway migrations, secure HTTP defaults, and optional Redis caching without hiding the generated code behind a framework runtime.

Use it when you want a practical DDD starting point that is fully owned by your team after generation—not a library that controls your domain model.

## Why Atom Archetype

- **Clear DDD boundaries:** a framework-neutral domain module, application use cases and output ports, and replaceable infrastructure adapters.
- **Secure by default:** business APIs reject anonymous access; trusted identity headers are restricted to explicitly enabled `dev` and `test` profiles.
- **Tenant-safe contracts:** authenticated caller and tenant identifiers are explicit, and persistence/cache access is tenant-scoped.
- **Reliable persistence baseline:** MyBatis-Plus, a single Flyway schema source, optimistic locking, and MySQL Testcontainers coverage.
- **Transaction-aware side effects:** cache changes and in-process domain-event publication run after a successful commit.
- **Optional infrastructure:** Redis is disabled by default and replaced by a no-op cache adapter, so it is not required for application correctness or startup.
- **Generated, readable code:** the result is an ordinary Maven reactor that can be changed, simplified, or extended without generator lock-in.

## Quick start

### Requirements

- JDK 21
- Maven 3.9 or newer
- Docker with Docker Compose v2 for the local MySQL service and integration tests

### Release status

The architecture documented here targets **2.0.0-SNAPSHOT** and is not yet published to Maven Central. Maven Central `1.1.0` is the legacy Spring Boot 3.5 architecture; it does not contain the security, tenancy, Flyway, or command/query changes described on this page.

Install the current snapshot locally first:

```bash
git clone https://github.com/Archetom/atom-archetype.git
cd atom-archetype
make install
cd ..
```

### 1. Generate a project from the installed snapshot

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.0.0-SNAPSHOT \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

### 2. Start MySQL and build

```bash
cd orders-service
docker compose up -d mysql
sh ./mvnw clean install
```

Flyway creates and validates the schema when the application starts. Redis is optional; do not start it unless you enable the Redis feature.

### 3. Run in explicit development mode

```bash
SPRING_PROFILES_ACTIVE=dev \
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
sh ./mvnw -f start/pom.xml spring-boot:run
```

Check the public health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Call a protected sample endpoint with the development-only identity headers:

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -H 'X-Dev-User-Id: 1001' \
  -H 'X-Dev-Tenant-Id: 42' \
  -d '{
    "username": "alice_01",
    "email": "alice@example.com",
    "password": "correct-horse-battery-staple",
    "realName": "Alice"
  }'
```

See [Getting started](docs/getting-started.md) for Redis, testing, production configuration, and authentication integration.

## Generated architecture

```text
REST / Facade adapters
          │
          ▼
   Application use cases ─────► output ports
          │                         ▲
          ▼                         │ implemented by
 Framework-neutral domain     Persistence / external adapters

                 start = composition root
```

| Module | Responsibility |
|---|---|
| `api` | Public request/response contracts, facade interfaces, authenticated caller context |
| `domain` | Aggregates, value objects, domain events, policies, repository/service ports |
| `shared` | Framework-neutral result and error conventions |
| `application` | Use-case orchestration, command/query templates, transaction hooks, output ports |
| `infra/rest` | Spring MVC controllers, Spring Security, OpenAPI, HTTP error mapping |
| `infra/persistence` | MyBatis-Plus repositories, Flyway migrations, optional Redis adapters |
| `infra/external` | Third-party adapters implementing application output ports |
| `infra/security` | Password hashing and other security technology adapters |
| `infra/facade` | Implementations of published facade contracts |
| `start` | Spring Boot entry point and runtime assembly |

The critical rule is simple: **the domain never depends on infrastructure**. See [Architecture](docs/architecture.md) for the complete dependency and transaction model.

## Security defaults

- `/api/**` requires authentication; user endpoints also require `users:read`, `users:write`, or `users:delete`.
- `/actuator/health` is anonymous. OpenAPI/Swagger endpoints are anonymous when enabled, but production disables them by default.
- `X-Dev-User-Id` and `X-Dev-Tenant-Id` are accepted only when a `dev` or `test` profile is active and trusted-header authentication is explicitly enabled.
- Production configuration disables trusted headers. Integrate your IdP through Spring Security and map the verified principal to `AuthenticatedCaller`.
- Production datasource URL, username, and password have no insecure defaults.
- Redis is off by default; enabling it is an explicit operational choice.

These defaults establish a safe boundary, but generated sample authorization rules and domain policies must still be adapted to your product.

## Compatibility

| Component | 2.0 baseline |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.x |
| Maven | 3.9+ recommended |
| MySQL | 8.4.10 used by Docker Compose; compatible MySQL 8 deployments should be verified |
| Redis | 7.4.9 optional |
| MyBatis-Plus | 3.5.16 |
| SpringDoc OpenAPI | 3.0.3 |

The verified deployment target is the JVM. GraalVM native-image support is intentionally not generated until it has a maintained compatibility test.

The current generation command pins the locally installed `2.0.0-SNAPSHOT`. After `2.0.0` is published, replace it with the exact released version for reproducible generation. Existing generated projects are not rewritten automatically when the archetype changes—follow the [Upgrade guide](docs/upgrade-guide.md).

## Documentation

- [Getting started](docs/getting-started.md)
- [Architecture and dependency rules](docs/architecture.md)
- [Naming conventions](docs/naming-conventions.md)
- [Upgrade guide](docs/upgrade-guide.md)
- [Release checklist](docs/releasing.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)
- [AI/LLM project index](llms.txt)

## Maintaining the archetype

Template sources live in `src/main/resources/archetype-resources/`; Maven archetype metadata lives in `src/main/resources/META-INF/maven/archetype-metadata.xml`.

```bash
make install
make demo
cd ~/Downloads/atom-demo
sh ./mvnw compile
CI=true sh ./mvnw test   # requires Docker
```

After every template change, verify both archetype generation and the generated reactor. Read [AGENTS.md](AGENTS.md) before changing Velocity-filtered templates.

Issues and pull requests are welcome at the [GitHub repository](https://github.com/Archetom/atom-archetype).

## License

[MIT](LICENSE)
