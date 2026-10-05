# Changelog

All notable architecture, compatibility, and security changes are documented here. Atom Archetype follows semantic versioning for generated-project contracts.

## [Unreleased]

## [2.2.0] — 2026-10-05

### Added

- HTTP bearer JWT authentication whenever a JWT decoder is configured (for example `spring.security.oauth2.resourceserver.jwt.issuer-uri`); HTTP and gRPC share one claim mapping (`atom.security.jwt.*`) to `ActorPrincipal`, and startup warns when no authentication is configured
- optional `infra/grpc` module that serves the facades over gRPC with Spring Boot's built-in gRPC support; the server is disabled by default (`atom.grpc.enabled=false`), authenticates every call with a bearer JWT mapped to `ActorPrincipal`, keeps the standard health service public, and maps stable application errors to gRPC statuses with a `google.rpc.ErrorInfo` reason
- generated projects pin `guava` and `error_prone_annotations` so atom-common and grpc-java pass dependency convergence

### Security

- generated projects manage Guava at `33.7.2-jre`, fixing uncontrolled memory allocation during native Java deserialization (GHSA-xxph-c9ww-hj94)
- **breaking:** facade contracts no longer accept `AuthenticatedCaller`, so they can be served over RPC without letting clients choose their identity, tenant, or authorities; `UserFacadeImpl` resolves the verified caller through the new `AuthenticatedCallerResolver`
- moved the generated test profile, which enables trusted development headers, to `start/src/test/resources` so it is never packaged into the application jar

### Changed

- value objects reject caller input with `InvalidValueException` (`DomainError.INVALID_VALUE`, HTTP 400); a bare `IllegalArgumentException` now signals a programming error and maps to HTTP 500
- **breaking:** moved `AuthenticatedCaller` from `api.context` to `application.security`, and `ActorPrincipal` from `infra/rest` to `infra/security`; `AuthenticatedCallerResolver` replaces `AuthenticatedCallerMapper`
- unexpected failures log the full exception at ERROR, including message and stack trace; public error responses still never expose internal details
- every HTTP response carries an `X-Request-Id` header, and log lines include the thread, request ID, and logger name; the request ID follows async event listeners
- added `ServiceOperation.resolveWithoutTransaction` so results such as cache hits are served before a transaction opens; User detail cache hits no longer borrow a database connection
- **breaking:** HTTP route authorities are declared by `ApiRouteAuthorization` beans instead of rules inside `SecurityConfig`; the sample `UserRouteAuthorization` and the use cases share the `UserAuthorities` constants, and `make clean-sample` deletes the sample rule instead of editing `SecurityConfig`
- **breaking:** `@Async` work runs on Spring Boot's application task executor, sized with `spring.task.execution.*`; the custom `task.executor.*` properties and `TaskExecutorProperties` are removed, and `TaskConfig` only adds the logging-context `TaskDecorator`
- the sample `t_user` migration names the hash column `password_hash` instead of `password`
- renamed `UserDomainService.encryptPassword` to `hashPassword`; `UserAlreadyExistsException.byUsername()`/`byEmail()` and `UserNotFoundException()` no longer take arguments they never used
- `UserFactory` validates the password policy before querying the repository for username and email uniqueness
- shared MyBatis-Plus and logging settings live once in `application.yml`; the test profile no longer overrides Jackson, transaction, or MyBatis-Plus settings, so integration tests run with the production configuration
- `make run` runs `start` with `-pl start -am spring-boot:run`, compiling the current sources without a prior `install`; only `start` is runnable
- integration tests start the MySQL image pinned in `docker-compose.yml`, and Dependabot no longer proposes MySQL major upgrades
- `infra/rest` declares `spring-boot-starter-validation` itself instead of relying on SpringDoc to bring a Bean Validation provider
- the archetype CI runs the fast suite during `install` and the Docker suite once; the no-op dependency analysis step is removed

### Fixed

- corrected the version guide to reflect that `2.0.0` is available from Maven Central
- keep the generic `DomainEventPublisherImpl` when `make clean-sample` removes the User sample
- report Spring MVC client errors, such as unknown routes (404) and unsupported methods (405), with their own status instead of HTTP 500
- treat stored rows that fail domain reconstruction as internal data-integrity failures rather than caller errors
- `make clean-sample` keeps the generic `RestExceptionAdviceTest`, `PageUtilTest`, and `ResultUtilTest`
- the getting-started example password now satisfies the 12-character password policy
- removed the malformed `.gitignore` entry and the empty resource file sets that made generation warn "Don't override file"
- removed hard-coded component versions from the READMEs and guides; the POMs and `docker-compose.yml` are the version sources

### Removed

- unused template APIs: `OperationLogPolicy` with the three-argument `execute`, `QueryServiceTemplate.reject`, `ResultResponseException`, `ErrorUtil.makeErrorCode`, `UseCaseOperation.findByCode`, `PasswordPolicy.checkStrength`, `AggregateRoot.removeDomainEvent`, `UserPOConverter.toPOList`, and `User.activate`
- the explicit `spring-jdbc` and `atom-common` dependencies of `start`; both still reach the executable jar through the modules

## [2.1.0] — 2026-08-08

### Changed

- made Chinese the default repository and generated-project README while retaining English as `README.en.md`
- raised the repository, CI, release workflows, and generated-project baseline from JDK 21 to JDK 25
- updated the archetype build's Maven Clean and Resources plugins and the generated project's Dependency Plugin to their latest stable releases
- added the required Lombok-MapStruct annotation-processor binding for reliable generated mappings on modern JDKs
- updated the generated Docker defaults to MySQL 26.7.0 and optional Redis 8.10.0
- updated generated projects to atom-common 1.1.0, MyBatis-Plus 3.5.17, and SpringDoc OpenAPI 3.1.0
- updated Maven JAR/source plugins and GitHub checkout actions
- aligned both Maven Enforcer rules and generated-project documentation with the bundled Maven 3.9.16 wrapper; all explicitly pinned third-party dependencies remain on their latest stable releases
- aligned Testcontainers and compatibility documentation with the generated Docker defaults
- added a protected Central Portal snapshot workflow with public resolution verification
- made release publication reject an existing Central version and verify the new artifact from a clean Maven repository

### Fixed

- include `atom-common` and Spring JDBC on the generated executable application's runtime classpath
- document that existing MySQL data volumes must follow MySQL's supported upgrade path before adopting newer generated Docker defaults
- preserve Maven, Spring, Logback, MyBatis, JSONPath, Markdown, and banner literals through Velocity filtering
- execute queries in independent read-only `REPEATABLE_READ` transactions and keep validation outside the snapshot
- expose a named `UserPageResponse` so OpenAPI clients retain the page item type
- map database lock acquisition failures to stable `CONCURRENT_OPERATION` errors and HTTP 409
- preserve already-classified operation errors at the REST boundary and provide a safe background logging policy
- compile and test a freshly generated project during the archetype integration-test lifecycle

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
