# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Atom Archetype is a **Maven archetype** that scaffolds DDD (Domain-Driven Design) based Java applications using Spring Boot 3.5+ and JDK 21. It generates a multi-module Maven project with layered architecture. The archetype itself is published to Maven Central under `io.github.archetom:atom-archetype`.

All template source code lives under `src/main/resources/archetype-resources/`. Files there use Maven archetype velocity variables (`${groupId}`, `${artifactId}`, `${package}`, `${rootArtifactId}`, `${version}`) — these are **not** typos.

## Build Commands

```bash
make install      # Clean build + install locally (skips GPG signing)
make clean        # Clean build artifacts
make deploy       # Deploy to Maven Central (requires GPG key)
make version VERSION=x.y.z  # Set project version
make demo         # Generate a demo project from the archetype into ~/Downloads/atom-demo
```

The underlying build is `mvn install -U -Dgpg.skip=true`. Default Maven goal is `package`.

## Archetype Structure

The archetype metadata is at `src/main/resources/META-INF/maven/archetype-metadata.xml`. This controls which files/modules get included in generated projects and how velocity filtering is applied.

Generated projects have these modules (defined in `archetype-resources/pom.xml`):

| Module | Purpose | Dependency Direction |
|---|---|---|
| `api` | External API declarations (DTO, Facade, Request/Response) | Depended on by application |
| `application` | Service orchestration, business workflows | Depends on domain, api |
| `domain` | Core entities, repository interfaces, domain services | **Independent — no framework deps** |
| `infra/persistence` | MyBatis-Plus mappers, repository implementations | Depends on domain |
| `infra/rest` | REST controllers | Depends on application |
| `infra/external` | External service integrations | Depends on domain |
| `infra/messaging` | Message queue consumers/producers | Depends on domain |
| `infra/rpc` | RPC service implementations | Depends on application |
| `shared` | Cross-cutting utilities, exceptions, constants | Depended on by all layers |
| `start` | Spring Boot main class, configuration | Aggregates all modules |

**Critical rule**: `domain` never depends on `infra`. Dependencies flow inward.

## Key Architectural Patterns in Templates

- **ServiceTemplate**: All business operations use `serviceTemplate.execute(EventEnum, ServiceCallback)` with a responsibility chain (param validation → context → concurrency → business → persistence → post-processing)
- **Object conversion flow**: Request → DTO (Assembler) → Entity (DomainService) → PO (Converter), and reverse for responses
- **Exception types**: `AppException` (retryable) and `AppUnRetryException` (non-retryable), both use `ErrorCodeEnum`
- **Repository pattern**: Interfaces in `domain`, implementations in `infra/persistence`

## Class Naming Conventions in Templates

- `*Request` / `*Response` — API layer DTOs
- `*DTO` — Data transfer objects between layers
- `*VO` — View objects (application layer)
- `*PO` — Persistence objects (infra layer)
- `*Assembler` — Cross-layer object converters (MapStruct)
- `*Converter` — Entity ↔ PO converters (MapStruct)
- `*DomainService` — Domain service interfaces/implementations

## Tech Stack (Generated Projects)

Spring Boot 3.5.4, MyBatis-Plus 3.5.12, MySQL 9.4.0, Druid connection pool, Redis (Lettuce), MapStruct 1.6.3, Lombok, SpringDoc OpenAPI 2.8.9, Kotlin 2.2.0 (optional), Testcontainers for integration tests. Core framework dependency: `io.github.archetom:atom-common:1.0.0`.

## Configuration

Generated projects use Spring profiles (`application-dev.yml`, `application-test.yml`, `application-prod.yml`) stored in the `conf/` directory. Docker Compose is provided for local MySQL and Redis.

## Working with This Repo

When editing archetype template files, remember:
1. Java files under `archetype-resources/` use velocity-filtered `${package}` for package declarations — preserve these
2. POM files use `${groupId}`, `${artifactId}`, `${rootArtifactId}`, `${version}` — these are archetype variables, not Maven properties
3. Test the archetype by running `make demo` and verifying the generated project compiles
4. The `archetype-metadata.xml` must be updated if new files or modules are added
