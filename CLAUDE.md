# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Atom Archetype is a **Maven archetype** that scaffolds DDD (Domain-Driven Design) based Java applications using Spring Boot 3.5+ and JDK 21. It generates a multi-module Maven project with layered architecture. The archetype itself is published to Maven Central under `io.github.archetom:atom-archetype`.

All template source code lives under `src/main/resources/archetype-resources/`. Files there use Maven archetype Velocity variables (`${groupId}`, `${artifactId}`, `${package}`, `${rootArtifactId}`, `${version}`) — these are **not** typos. Some template files also use `#set( $symbol_dollar = '$' )` to escape dollar signs that should appear literally in the generated output (e.g., Spring `${SOME_ENV_VAR}` placeholders in YAML).

## Build Commands

```bash
make install      # Clean build + install locally (skips GPG signing) — DEFAULT target
make clean        # Clean build artifacts
make deploy       # Deploy to Maven Central (requires GPG key)
make version VERSION=x.y.z  # Set project version
make demo         # Generate a demo project from the archetype into ~/Downloads/atom-demo
```

**Verification workflow**: after any template change, always run `make install && make demo`, then `cd ~/Downloads/atom-demo && mvn compile` (and `mvn test` for domain tests) to confirm the generated project compiles.

## Archetype Structure

The archetype metadata is at `src/main/resources/META-INF/maven/archetype-metadata.xml`. This controls which files/modules get included in generated projects and how Velocity filtering is applied.

Generated projects have these modules (defined in `archetype-resources/pom.xml`):

| Module | Purpose | Dependency Direction |
|---|---|---|
| `api` | External API declarations (DTO, Facade, Request/Response) | Depended on by application |
| `application` | Service orchestration, business workflows, async config | Depends on domain, api |
| `domain` | Core entities, repository interfaces, domain services | Minimal deps (spring-security-crypto for BCrypt) |
| `infra/persistence` | MyBatis-Plus mappers, repository implementations, Redis lock | Depends on domain |
| `infra/rest` | REST controllers, interceptors, WebMvc config | Depends on application |
| `infra/external` | External service integrations (email, SMS) | Depends on domain |
| `infra/messaging` | Message queue consumers/producers, event publishing | Depends on domain |
| `infra/facade` | Facade service implementations (API contract impl) | Depends on application |
| `shared` | Cross-cutting utilities, exceptions, constants, ServiceTemplate | Depended on by all layers |
| `start` | Spring Boot main class, configuration, integration tests | Aggregates all modules |

**Critical rule**: `domain` never depends on `infra`. Dependencies flow inward.

## Key Architectural Patterns in Templates

- **ServiceTemplate**: All business operations use `serviceTemplate.execute(EventEnum, ServiceCallback)` with a responsibility chain (param validation → context → concurrency → business → persistence → post-processing)
- **Object conversion flow**: Request → DTO (Assembler) → Entity (DomainService) → PO (Converter), and reverse for responses
- **Exception types**: `AppException` (retryable) and `AppUnRetryException` (non-retryable), both use `ErrorCodeEnum`
- **Repository pattern**: Interfaces in `domain`, implementations in `infra/persistence`
- **Entity design**: Entities use `@Getter` (no `@Data`/setters), mutation through business methods only. Use `User.reconstitute(...)` static method for persistence-layer reconstruction, factory methods for creation
- **AggregateRoot**: Base class with domain event collection and optimistic lock `version` field
- **UserContext propagation**: `UserContextHolder` (ThreadLocal) in `domain.context` package; `ContextCopyTaskDecorator` copies context to async threads
- **API versioning**: REST endpoints under `/api/v1/`

## Class Naming Conventions in Templates

- `*Request` / `*Response` — API layer DTOs
- `*DTO` — Data transfer objects between layers
- `*VO` — View objects (application layer)
- `*PO` — Persistence objects (infra layer), extend `BasePO` (with `@Version` optimistic lock)
- `*Assembler` — Cross-layer object converters (MapStruct)
- `*Converter` — Entity ↔ PO converters (MapStruct, abstract class with manual `toDomain()` using `reconstitute`)
- `*DomainService` — Domain service interfaces/implementations

## Tech Stack (Generated Projects)

Spring Boot 3.5.4, MyBatis-Plus 3.5.12, MySQL 9.4.0, Druid connection pool, Redis (Lettuce), MapStruct 1.6.3, Lombok, SpringDoc OpenAPI 2.8.9, Kotlin 2.2.0 (optional), Testcontainers for integration tests. Core framework dependency: `io.github.archetom:atom-common:1.0.0`.

## Configuration

Generated projects use Spring profiles (`application-dev.yml`, `application-test.yml`, `application-prod.yml`) stored in the `conf/` directory. Docker Compose is provided for local MySQL and Redis. The prod profile uses environment variable placeholders (e.g., `${SPRING_DATASOURCE_URL}`).

## Working with This Repo

When editing archetype template files, remember:
1. Java files under `archetype-resources/` use velocity-filtered `${package}` for package declarations — preserve these
2. POM files use `${groupId}`, `${artifactId}`, `${rootArtifactId}`, `${version}` — these are archetype variables, not Maven properties
3. YAML/config files under `conf/` are also Velocity-filtered; use `#set( $dollar = '$' )` and `${dollar}{ENV_VAR}` to output literal `$` signs
4. Test the archetype by running `make demo` and verifying the generated project compiles
5. The `archetype-metadata.xml` must be updated if new files or modules are added
6. The `clean.sh` script lists all example files for deletion — update it when adding/removing/renaming template files
