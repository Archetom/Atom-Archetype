# Changelog

All notable architecture, compatibility, and security changes are documented here. Atom Archetype follows semantic versioning for generated-project contracts.

## [Unreleased]

### Changed

- made Chinese the default repository and generated-project README while retaining English as `README.en.md`
- raised the repository, CI, release workflows, and generated-project baseline from JDK 21 to JDK 25
- updated the archetype build's Maven Clean and Resources plugins and the generated project's Dependency Plugin to their latest stable releases
- added the required Lombok-MapStruct annotation-processor binding for reliable generated mappings on modern JDKs
- moved the development line to `2.1.0-SNAPSHOT`
- updated the generated Docker defaults to MySQL 9.7.1 LTS and optional Redis 8.8.0
- updated Maven JAR/source plugins and GitHub checkout actions
- aligned both Maven Enforcer rules and generated-project documentation with the bundled Maven 3.9.16 wrapper; all explicitly pinned third-party dependencies remain on their latest stable releases
- aligned Testcontainers and compatibility documentation with the generated Docker defaults
- added a protected Central Portal snapshot workflow with public resolution verification

### Fixed

- include `atom-common` and Spring JDBC on the generated executable application's runtime classpath
- document that existing MySQL data volumes must reach 8.4 LTS before upgrading to 9.7 LTS

## [2.0.0] — 2026-07-10

This is a breaking release of the generated-project architecture and public API contracts.

### Added

- Spring Boot 4.1 and Jackson 3 compatible dependencies
- secure-by-default Spring Security boundary and explicit `AuthenticatedCaller`
- mandatory `TenantId` propagation through repositories and caches
- Flyway as the only schema source, optimistic locking, and MySQL integration tests
- after-commit cache/event handling and owner-specific distributed lock handles
- `CommandServiceTemplate`, `QueryServiceTemplate`, and typed `ServiceOperation`
- AI-oriented `AGENTS.md` and `llms.txt` documentation in generated projects
- generated-project CI that compiles and tests a freshly generated reactor

### Changed

- development version moved from the 1.1 patch line to 2.0 because module, API, security, configuration, and database contracts are incompatible
- application errors use `ApplicationErrorCode`, `ApplicationException`, and `NonRetryableApplicationException`
- application ports own cache/lock contracts; technology implementations live in infrastructure modules
- the fake messaging adapter and redundant MyBatis DAO layer were removed

### Security

- removed caller-controlled role/admin headers and domain ThreadLocal identity
- trusted development headers require an explicit dev/test profile and cannot load with prod
- production datasource credentials have no local fallbacks
- internal exception details and sensitive request fields are no longer returned or logged

Migration details are in [docs/upgrade-guide.md](docs/upgrade-guide.md).

## [1.1.0] — 2025

Legacy Maven Central release based on Spring Boot 3.5. It does not contain the 2.0 architecture documented on `main`.
