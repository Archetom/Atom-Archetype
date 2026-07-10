# Changelog

All notable architecture, compatibility, and security changes are documented here. Atom Archetype follows semantic versioning for generated-project contracts.

## [Unreleased] — 2.0.0

This is a breaking release and is currently available only as `2.0.0-SNAPSHOT` after a local `make install`.

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
