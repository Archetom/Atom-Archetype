# Atom Archetype

[![Maven Central](https://img.shields.io/maven-central/v/io.github.archetom/atom-archetype.svg)](https://central.sonatype.com/artifact/io.github.archetom/atom-archetype)
[![CI](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml/badge.svg)](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml)
[![Java 27](https://img.shields.io/badge/Java-27-007396.svg)](https://openjdk.org/projects/jdk/27/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[简体中文](README.md) | English

Atom Archetype is a Maven archetype for generating multi-module projects based on Domain-Driven Design (DDD). The current `main` branch uses Java 27 and Spring Boot 4.1.

The generated result is a standard Maven project. Dependency boundaries between domain, application, and infrastructure modules are already configured, and the example code can be changed or removed directly.

The current `main` branch also supports AI-assisted development with Agent Skills, incremental validation, and independent business-task benchmarks. These reuse the existing architecture and testing tools without adding application runtime dependencies.

## Versions

- `main` is unreleased; archetype development and generated projects require JDK 27 and Maven 3.10.0. Java 27 is a non-LTS release; see the [upgrade guide](docs/upgrade-guide.md) for the toolchain changes.
- `2.2.0` is the current stable release and uses JDK 25. It contains breaking changes, so read the [upgrade guide](docs/upgrade-guide.md) before upgrading from `2.1.0`.
- [`v2.0.0`](https://github.com/Archetom/Atom-Archetype/tree/v2.0.0) is the older Java 21 tag.
- Maven Central `1.1.0` uses the legacy Spring Boot 3.5 architecture.

The quick start below uses `2.2.0` directly from Maven Central.

## Quick start

The stable `2.2.0` release needs JDK 25, Docker, and Docker Compose v2. Use Maven 3.9.16 or newer for generation, then use the generated project's bundled Maven Wrapper.

### 1. Generate a project

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.2.0 \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

### 2. Build and run

```bash
cd orders-service
docker compose up -d mysql
sh ./mvnw clean install

ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -pl start -am spring-boot:run -Dspring-boot.run.profiles=dev
```

After startup, check the health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

See [Getting started](docs/getting-started.md) for development identity headers, Redis, and production configuration.

## Generated contents

- Dependency boundaries between `domain`, `application`, `api`, and infrastructure modules.
- Explicit `AuthenticatedCaller` and `TenantId` values for tenant-scoped repository and cache access.
- MyBatis-Plus, Flyway, and MySQL.
- Spring Security, SpringDoc OpenAPI, and consistent HTTP error mapping.
- Redis adapters, disabled by default, with a corresponding no-op implementation.
- A gRPC server, disabled by default, built on Spring Boot's gRPC support, authenticated with bearer JWTs, and backed by the same facades as HTTP.
- Independent command transactions and read-only query snapshot transactions, after-commit callbacks, and Testcontainers integration tests.
- Named pagination responses, stable concurrency errors, and archetype regression tests that compile and test the generated project.

Exact dependency versions are pinned in the generated `pom.xml` and `docker-compose.yml`.

Business APIs require authentication by default. In production, configure the JWT issuer and both HTTP and gRPC authenticate bearer JWTs; development identity headers are limited to explicitly enabled `dev` and `test` environments.

## AI-assisted development

The capabilities below are part of the unreleased `main` branch, not the `2.2.0` release used in the quick start above. Follow [Development](#development) to generate a project from the current sources.

### Agent Skills

Generated projects define architecture, security, and verification rules in `AGENTS.md`, index documentation through `llms.txt`, and provide five Markdown workflows under `.agents/skills/` to read as needed:

| Skill | Purpose |
|---|---|
| [create-feature](src/main/resources/archetype-resources/.agents/skills/create-feature/SKILL.md) | Implement a business feature through domain behavior, application use cases, and the required adapters |
| [add-api](src/main/resources/archetype-resources/.agents/skills/add-api/SKILL.md) | Add or change an HTTP API, covering contracts, authorization, mapping, and regression tests |
| [add-migration](src/main/resources/archetype-resources/.agents/skills/add-migration/SKILL.md) | Evolve Flyway, persistence mappings, and aggregate reconstruction together, checking tenancy and optimistic locking |
| [review-architecture](src/main/resources/archetype-resources/.agents/skills/review-architecture/SKILL.md) | Review DDD boundaries, security, persistence, and transaction behavior |
| [verify-change](src/main/resources/archetype-resources/.agents/skills/verify-change/SKILL.md) | Select validation scopes, run checks, and report unverified behavior |

These workflows require no particular model or editor and remain after the User example is removed. ArchUnit architecture tests and existing unit and integration tests verify the corresponding constraints. See the [AI engineering workflow](docs/ai-engineering.md).

### Incremental validation and structured reports

Run from the repository root or the root of a project generated from `main`:

```bash
make agent-check SCOPE=affected  # Select checks conservatively from Git changes
make agent-check SCOPE=fast      # Explicit checks without Docker
make agent-check                # Default full scope, including Docker integration tests
```

`affected` inspects staged, unstaged, untracked, deleted, and moved files. In generated projects, it narrows checks only for recognized documentation changes or ordinary Java test changes within a single `domain` or `application` module. Template documentation changes in the archetype repository still require generation checks. Production code, POMs, cross-module changes, security or other sensitive changes, and uncertain cases fall back to full validation. The selector requires Python 3 and also falls back to `full` when Python is unavailable.

The existing `docs`, `generate` (repository only), `fast`, and `full` scopes remain available. Standard output is JSON with the actual scope, durations, exit codes, and paths to generated test reports; detailed logs go to standard error. Missing required Docker support produces a blocked result and a nonzero exit code. Passing `fast` does not certify integration tests. See the [verification guide](docs/ai-engineering.md#verification-entry-point) for the checks included in each scope.

### Business-task benchmarks

The repository's `benchmarks/` directory provides an offline evaluator for three tasks on projects generated from a fixed commit. Every run uses an isolated workspace and independent acceptance tests after submission:

| Scenario | Task and acceptance focus |
|---|---|
| [add-api](benchmarks/scenarios/add-api/task.md) | Update a user's email: input validation, authentication, permissions, tenant isolation, and persisted results |
| [domain-invariant](benchmarks/scenarios/domain-invariant/task.md) | Require review before activating a locked user: aggregate transitions, rejected operations, and existing behavior |
| [persistence-change](benchmarks/scenarios/persistence-change/task.md) | Add a preferred language: Flyway upgrades, mapping and reconstruction, query output, tenant isolation, and optimistic locking |

The evaluator makes no model API calls; operators can run their chosen coding agent manually. JSON and Markdown results record task success, first-pass success, architecture and security violations, required-behavior acceptance coverage, duration, and other metrics. Unobserved token usage, human interventions, and similar data are marked `unavailable`. Compilation alone is insufficient for task success.

Both `baseline` and `engineering` retain `AGENTS.md`, so the comparison measures the incremental contribution of the additional Skills. Keep the source revision, model version, and execution environment identical. Evaluator calibration is not evidence of model capability or efficiency gains. Normal CI runs deterministic tests; real agent evaluations are optional, and the benchmark adds no production dependency to generated projects. See the [benchmark guide](benchmarks/README.md) for preparation, submission, evaluation, and comparison commands.

## Project structure

| Module | Responsibility |
|---|---|
| `api` | Public requests, responses, and facade contracts shared by HTTP and RPC clients, without identity parameters |
| `domain` | Aggregates, value objects, domain events, repository and domain-service ports |
| `shared` | Framework-neutral result and error types |
| `application` | Use-case orchestration, command/query templates, transaction callbacks, and output ports |
| `infra/rest` | Spring MVC, Spring Security, OpenAPI, and error mapping |
| `infra/persistence` | MyBatis-Plus, Flyway, and cache adapters |
| `infra/external` | Third-party system adapters |
| `infra/security` | Password hashing and other security adapters |
| `infra/facade` | Facade contract implementations |
| `infra/grpc` | Optional gRPC server, disabled by default: proto contracts, JWT authentication, and status mapping |
| `start` | Spring Boot entry point and runtime assembly |

`domain` does not depend on `application`, `api`, `shared`, or any `infra` module. See [Architecture](docs/architecture.md) for the complete rules.

## Documentation

- [Getting started](docs/getting-started.md)
- [Architecture and dependency rules](docs/architecture.md)
- [Naming conventions](docs/naming-conventions.md)
- [AI engineering workflow and verification scopes](docs/ai-engineering.md)
- [Agent benchmark guide](benchmarks/README.md)
- [Upgrade guide](docs/upgrade-guide.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Changelog](CHANGELOG.md)

## Development

Development on `main` requires JDK 27. The repository and projects generated from it use Maven Wrapper 3.10.0; system Maven installations must also be 3.10.0 or newer. Template sources live in `src/main/resources/archetype-resources/`; metadata lives in `src/main/resources/META-INF/maven/archetype-metadata.xml`.

```bash
CI=false make demo
cd target/generated-projects/atom-demo
sh ./mvnw compile
CI=true sh ./mvnw test   # requires Docker
```

After a template change, verify both archetype generation and the generated Maven reactor.

## Contributing

Read the [contribution guide](CONTRIBUTING.md) and [security policy](SECURITY.md) before submitting a change. Issues and pull requests are welcome in this repository.

## License

[MIT](LICENSE)
