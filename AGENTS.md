# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

Atom Archetype is a **Maven archetype** that scaffolds DDD (Domain-Driven Design) Java applications using Spring Boot 4.1 and JDK 25. It generates a multi-module Maven project with layered/ports-and-adapters boundaries. The current architecture is released as `io.github.archetom:atom-archetype:2.0.0`; Maven Central `1.1.0` is the legacy Boot 3.5 line.

All template source code lives under `src/main/resources/archetype-resources/`. Files there use Maven archetype Velocity variables (`${groupId}`, `${artifactId}`, `${package}`, `${rootArtifactId}`, `${version}`) — these are **not** typos. Filtered config files use `#set( $dollar = '$' )` and `${dollar}{SOME_ENV_VAR}` when the generated output needs a literal Spring `${SOME_ENV_VAR}` placeholder.

## Build Commands

```bash
make install      # Clean build + install locally (skips GPG signing) — DEFAULT target
make clean        # Clean build artifacts
make deploy       # Deploy to Maven Central (requires GPG key)
make version VERSION=x.y.z  # Set project version
make demo         # Generate a demo project from the archetype into ~/Downloads/atom-demo
```

**Verification workflow**: after any template change, always run `make install && make demo`, then `cd ~/Downloads/atom-demo && sh ./mvnw compile`. Run `sh ./mvnw test` for unit tests and `CI=true sh ./mvnw test` with Docker available for the supplied Testcontainers integration tests.

## Archetype Structure

The archetype metadata is at `src/main/resources/META-INF/maven/archetype-metadata.xml`. This controls which files/modules get included in generated projects and how Velocity filtering is applied.

Generated projects have these modules (defined in `archetype-resources/pom.xml`):

| Module | Purpose | Dependency Direction |
|---|---|---|
| `api` | External DTO/facade contracts and `AuthenticatedCaller` | Depended on by application/inbound adapters |
| `application` | Use cases, command/query execution, after-commit hooks, output ports | Depends on domain, api, shared |
| `domain` | Framework-neutral aggregates, value objects, events, repositories, domain services | No Spring/API/infra dependencies |
| `infra/persistence` | MyBatis-Plus repository adapters, Flyway migrations, optional Redis adapters | Depends on domain and application ports |
| `infra/rest` | REST controllers, Spring Security, OpenAPI, HTTP error mapping | Depends on application, API/domain contracts, shared boundary types |
| `infra/external` | Third-party adapters for application output ports | Depends on application |
| `infra/security` | Password hashing and other security adapters | Depends on domain ports |
| `infra/facade` | Facade service implementations (API contract impl) | Depends on application and API |
| `shared` | Framework-neutral result and error conventions | Used by boundary/application modules, not domain |
| `start` | Spring Boot main class, configuration, integration tests | Aggregates all modules |

**Critical rule**: `domain` never depends on `application`, `api`, `shared`, or `infra`. Dependencies flow inward toward domain/application contracts.

## Key Architectural Patterns in Templates

- **Application operation templates**: Commands and queries use `CommandServiceTemplate` / `QueryServiceTemplate` with a typed `ServiceOperation` lifecycle (`validate → prepare → execute → onSuccess`) and application-owned `UseCaseOperation` identifiers; command transactions wrap `execute/onSuccess`
- **Object conversion flow**: Request enters the application use case directly; `*Assembler` maps aggregate → VO → tenant-safe Response, while `*POConverter` maps aggregate ↔ PO
- **Exception types**: Domain failures use `DomainException`/`DomainError`; application failures use `ApplicationException` and `NonRetryableApplicationException` and are mapped at the boundary
- **Repository pattern**: Interfaces in `domain`, implementations in `infra/persistence`
- **Entity design**: Entities use `@Getter` (no `@Data`/setters), mutation through business methods only. Use `User.reconstitute(UserSnapshot)` for persistence-layer reconstruction, factory methods for creation
- **AggregateRoot**: Base class with domain event collection and optimistic-lock `version`; persistence restores version without producing events
- **Explicit caller/tenant**: `AuthenticatedCaller` and `TenantId` are required inputs; repositories and cache keys are tenant-scoped and do not read a ThreadLocal
- **Post-commit side effects**: `AfterCommitExecutor` delays cache changes and in-process event publication until a successful transaction commit; register events and cache work independently so one failure cannot suppress the other; durable cross-service delivery still requires an outbox
- **Optional Redis**: `atom.redis.enabled=false` selects a no-op cache adapter, so Redis is not a correctness or startup requirement
- **API versioning**: REST endpoints under `/api/v1/`

## Class Naming Conventions in Templates

- `*Request` / `*Response` — API layer DTOs
- `*VO` — Application result/view objects
- `*PO` — Persistence objects (infra layer), extend `BasePO` (with `@Version` optimistic lock)
- `*Assembler` — Cross-layer object converters (MapStruct)
- `*POConverter` — Entity ↔ PO converters (MapStruct, abstract class with manual `toDomain()` using a named reconstruction snapshot)
- `*DomainService` — Domain services for behavior that does not belong to a single aggregate/value object
- `*Repository` / `*RepositoryImpl` — Domain port / infrastructure adapter
- `UseCaseOperation` — Application-owned use-case/error-scene identifiers; enums do not use an `Enum` suffix

## Tech Stack (Generated Projects)

Spring Boot 4.1, Java 25, MyBatis-Plus 3.5.16, MySQL 9.7.1 LTS in Docker Compose, Flyway, optional Redis 8.8.0 (Lettuce), MapStruct 1.6.3, Lombok, SpringDoc OpenAPI 3.0.3, and Testcontainers. Core result/error dependency: `io.github.archetom:atom-common:1.0.0`.

## Configuration

Generated projects use explicit Spring profiles (`application-dev.yml`, `application-test.yml`, `application-prod.yml`) stored in `conf/`; no profile is activated implicitly. Docker Compose provides MySQL and optional Redis. Production requires datasource environment variables and disables development trusted headers. Flyway migrations under `infra/persistence/src/main/resources/db/migration` are the only schema source.

## Working with This Repo

When editing archetype template files, remember:
1. Java files under `archetype-resources/` use velocity-filtered `${package}` for package declarations — preserve these
2. POM files use `${groupId}`, `${artifactId}`, `${rootArtifactId}`, `${version}` — these are archetype variables, not Maven properties
3. YAML/config files under `conf/` are Velocity-filtered; use `#set( $dollar = '$' )` and `${dollar}{ENV_VAR}` to output literal `$` signs
4. Test the archetype by running `make demo` and verifying the generated project compiles
5. The `archetype-metadata.xml` must be updated if new files or modules are added
6. The `clean.sh` script lists all example files for deletion — update it when adding/removing/renaming template files
7. Add schema changes as new Flyway migrations; do not add a second `schema.sql`, Docker init schema, or test-only schema source
8. Never make trusted identity headers available in `prod`, accept an `X-Admin` role header, or reintroduce null-tenant/fail-open repository queries
