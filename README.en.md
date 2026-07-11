# Atom Archetype

[![Maven Central legacy release](https://img.shields.io/maven-central/v/io.github.archetom/atom-archetype.svg?label=Maven%20Central%20legacy)](https://central.sonatype.com/artifact/io.github.archetom/atom-archetype)
[![CI](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml/badge.svg)](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml)
[![Java 25](https://img.shields.io/badge/Java-25-007396.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[简体中文](README.md) | English

Atom Archetype is a Maven archetype for generating Java 25 and Spring Boot 4.1 multi-module projects based on Domain-Driven Design (DDD).

The generated result is a standard Maven project. Dependency boundaries between domain, application, and infrastructure modules are already configured, and the example code can be changed or removed directly.

## Versions

- `main` is currently `2.1.0-SNAPSHOT`; both archetype development and generated projects use JDK 25.
- [`v2.0.0`](https://github.com/Archetom/Atom-Archetype/tree/v2.0.0) is the stable Git tag for the current layered architecture and targets Java 21.
- Maven Central currently contains only `1.1.0`, which uses the legacy Spring Boot 3.5 architecture.

The quick start below uses `main`, so install `2.1.0-SNAPSHOT` in your local Maven repository first.

## Quick start

You need JDK 25, Docker, and Docker Compose v2. The repository and generated projects both include Maven Wrapper 3.9.16; use Maven 3.9 or newer if you prefer a system installation.

### 1. Install the archetype

```bash
git clone https://github.com/Archetom/Atom-Archetype.git
cd Atom-Archetype
./mvnw clean install -Dgpg.skip=true
```

### 2. Generate a project

```bash
cd ..
./Atom-Archetype/mvnw -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.1.0-SNAPSHOT \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

### 3. Build and run

```bash
cd orders-service
docker compose up -d mysql
sh ./mvnw clean install

ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

After startup, check the health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

See [Getting started](docs/getting-started.md) for development identity headers, Redis, and production configuration.

## Generated contents

- Dependency boundaries between `domain`, `application`, `api`, and infrastructure modules.
- Explicit `AuthenticatedCaller` and `TenantId` values for tenant-scoped repository and cache access.
- MyBatis-Plus 3.5.16, Flyway, and MySQL 9.7.1 LTS.
- Spring Security, SpringDoc OpenAPI 3.0.3, and consistent HTTP error mapping.
- Redis 8.8.0 adapters, disabled by default, with a corresponding no-op implementation.
- Command/query service templates, after-commit callbacks, and Testcontainers integration tests.

Business APIs require authentication by default. Development identity headers are limited to explicitly enabled `dev` and `test` environments; production should integrate its own identity provider.

## Project structure

| Module | Responsibility |
|---|---|
| `api` | Public requests, responses, facade contracts, and caller context |
| `domain` | Aggregates, value objects, domain events, repository and domain-service ports |
| `shared` | Framework-neutral result and error types |
| `application` | Use-case orchestration, command/query templates, transaction callbacks, and output ports |
| `infra/rest` | Spring MVC, Spring Security, OpenAPI, and error mapping |
| `infra/persistence` | MyBatis-Plus, Flyway, and cache adapters |
| `infra/external` | Third-party system adapters |
| `infra/security` | Password hashing and other security adapters |
| `infra/facade` | Facade contract implementations |
| `start` | Spring Boot entry point and runtime assembly |

`domain` does not depend on `application`, `api`, `shared`, or any `infra` module. See [Architecture](docs/architecture.md) for the complete rules.

## Documentation

- [Getting started](docs/getting-started.md)
- [Architecture and dependency rules](docs/architecture.md)
- [Naming conventions](docs/naming-conventions.md)
- [Upgrade guide](docs/upgrade-guide.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Changelog](CHANGELOG.md)

## Development

Template sources live in `src/main/resources/archetype-resources/`; metadata lives in `src/main/resources/META-INF/maven/archetype-metadata.xml`.

```bash
make install
make demo
cd ~/Downloads/atom-demo
sh ./mvnw compile
CI=true sh ./mvnw test   # requires Docker
```

After a template change, verify both archetype generation and the generated Maven reactor.

## Contributing

Read the [contribution guide](CONTRIBUTING.md) and [security policy](SECURITY.md) before submitting a change. Issues and pull requests are welcome in this repository.

## License

[MIT](LICENSE)
