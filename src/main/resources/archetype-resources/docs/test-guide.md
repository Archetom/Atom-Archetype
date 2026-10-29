# Testing guide

Test domain behavior without Spring, application orchestration through ports, adapters at their boundaries, and the assembled application against MySQL.

## Commands

Run the shared verification entry point from a generated project root:

```bash
make agent-check SCOPE=fast  # Unit and architecture tests; no Docker.
make agent-check            # Default full scope, requiring Docker.
make agent-check SCOPE=docs # Whitespace only; review facts and links separately.
make agent-check SCOPE=affected # Select conservatively from the Git change set.
```

The final JSON reports the scope and check results; command logs go to standard error. Missing Docker makes the full check return nonzero with a blocked result. A passing fast check does not cover the integration suite. `SCOPE=generate` belongs only to the archetype repository. These commands do not remove the generated application's business/sample code. The docs scope requires an independent Git root; a generated directory nested in the archetype checkout is blocked rather than checking the parent's changes.

`affected` is opt-in; the default remains `full`. It includes staged, unstaged, and untracked files and both paths of a rename. Only recognized documentation changes or eligible Java test changes in one domain/application module can narrow checks. Production code, POMs, public/shared contracts, cross-module changes, infrastructure, and security/tenant/transaction/cache/event-sensitive paths select full checks. Missing Git history, an unknown path, or an empty change set also selects full. This conservative path selection does not establish semantic safety; choose full explicitly when the changed behavior needs integration coverage.

The optional selector uses Python 3's standard library. If Python is unavailable, `affected` selects full checks; existing explicit scopes still need no Python. Inspect the final result's selected scope and reason before describing what was verified.

The result retains `status`, `context`, `scope`, and `checks` for existing callers, with additive timing, exit-code, selection, and report diagnostics. Use only report paths actually listed for the current invocation when locating test failures; stale or missing Maven reports are not proof of success. Retain the reported diagnostics with a failed run rather than attempting to infer Maven success from its logs.

Run fast tests:

```bash
CI=false sh ./mvnw test
```

Run one module and its dependencies:

```bash
CI=false sh ./mvnw -pl shared -am test
CI=false sh ./mvnw -pl domain -am test
CI=false sh ./mvnw -pl application -am test
CI=false sh ./mvnw -pl infra/persistence -am test
CI=false sh ./mvnw -pl infra/rest -am test
CI=false sh ./mvnw -pl infra/grpc -am test
```

Run Docker-backed integration tests:

```bash
CI=true sh ./mvnw test
```

`CI=true` enables the generated integration test classes. Docker must be available. Testcontainers starts the MySQL image pinned in `docker-compose.yml`, so tests and local development share one version; Redis remains disabled.

## Test layers

| Layer | Test focus | Typical dependencies |
| --- | --- | --- |
| Shared | Result mapping, safe public errors, and pagination metadata | JUnit Jupiter |
| Domain | Value validation, aggregate transitions, policies, events | JUnit and assertions only |
| Application | Authority checks, tenant propagation, orchestration, post-commit scheduling | Mocks or small port fakes |
| Persistence converter | Full PO/aggregate round trip, version, timestamps | MapStruct mapper instance |
| REST | Error/status mapping, bearer JWT and anonymous rejection, safe messages | Spring MVC and Security test support |
| Security adapter | Verified-caller resolution from Spring Security's context and fail-closed handling of missing principals | Spring Security core types |
| gRPC | Bearer-JWT authentication, public health checks, request validation, and gRPC status mapping | In-process gRPC transport and test-signed JWTs |
| Start integration | Flyway, MySQL, tenant isolation, locking, HTTP and gRPC flows | Spring Boot, MockMvc, in-process gRPC, Testcontainers |
| Architecture | Forbidden dependencies, adapter bypasses, entity setters, naming | ArchUnit bytecode inspection and Maven Enforcer |

Use the lowest layer that proves the behavior.

`ArchitectureBoundaryTest` checks compiled static boundaries, not the correctness of authentication, tenant SQL, or commit ordering. Those claims need the runtime tests below. For business rules that need context beyond code, record a stable invariant and its test in [domain contracts](domain/README.md).

### Shared

Keep shared tests free of Spring. Cover result mapping, fixed public messages for unexpected failures, error-context chaining, complete error-code formats, and pagination metadata separately from REST transport tests.

## Representative tests

### Domain

Create aggregates through their public factories and invoke behavior directly:

```java
@Test
void delete_active_user_changes_status_and_records_event() {
    User user = createUserForTenant(1L);

    user.delete();

    assertTrue(user.isDeleted());
    assertFalse(user.getDomainEvents().isEmpty());
}
```

Reconstitution tests also verify that persisted `version` is restored without new domain events. Domain tests do not use Spring, databases, HTTP, or mocked domain objects.

### Application

Test through output ports. Verify that an invalid caller or missing authority is rejected before repository access, and that the caller's `TenantId` reaches every repository and cache call.

For commands, verify that `validate` and `prepare` precede the independent transaction, and that a failure rolls back before it is converted to a `Result`. For queries, verify that the read phase uses an independent read-only `REPEATABLE_READ` transaction and that a cache hit resolved without a transaction opens none. Post-commit work runs after commit and does not run after rollback; register event and cache callbacks independently. `CommandServiceTemplateTest`, `QueryServiceTemplateTest`, and `AfterCommitExecutorTest` are the references.

### Persistence

`UserPOConverterTest` covers every persisted field in both directions, including tenant, password hash, flags, optimistic-lock version, audit timestamps, and reconstruction without events. It also verifies that `toString` does not expose the password hash. Extend it whenever `UserPO`, mapper XML, or a migration changes.

### Integration

`BaseIntegrationTest` supplies the `test` profile, MySQL Testcontainers properties, Flyway initialization, MockMvc identity helpers, and explicit database cleanup. Integration tests are not wrapped in one rollback transaction because real commits are needed for post-commit effects and optimistic locking.

Representative integration coverage includes:

- migration and PO round trips against an empty MySQL database;
- tenant isolation in repositories and cache keys;
- optimistic-lock and unique-key conflicts mapped to `409`;
- soft deletion through `status=DELETED` without physical row removal;
- authentication, capability separation, validation, and safe error responses;
- Redis-disabled health and generated OpenAPI metadata.

### Bearer JWT authentication

`TestTokens` signs JWTs with a per-run test key, and `@Import(TestTokens.TrustedIssuer.class)` makes the application trust it. `BearerTokenIntegrationTest` covers HTTP authentication and `GrpcIntegrationTest` covers gRPC; unit tests cover the claim mapping in `ActorJwtAuthenticationConverterTest`.

### Trusted-header authentication

The `test` profile in `start/src/test/resources/application-test.yml` enables the trusted-header adapter. It is a test resource, so the packaged application never contains it. Requests use positive numeric `X-Dev-User-Id` and `X-Dev-Tenant-Id` values.

Cover missing, incomplete, and invalid header pairs; missing authority; attempted authority or administrator escalation; and trusted headers combined with `prod`. Keep credentials out of assertion output and captured logs. These tests do not replace tests for the production authentication integration.

## Change-to-test map

| Change | Minimum verification |
| --- | --- |
| Aggregate or value object | Domain unit test |
| Shared result, error, or pagination contract | Shared JUnit contract test |
| Application command/query | Caller, tenant, success, and failure application tests |
| Cache behavior | Tenant-key and database fallback tests |
| PO or converter | Complete round-trip test |
| Mapper query | MySQL integration test |
| Flyway migration | Empty-database migration test |
| Security route | Anonymous, allowed, and forbidden HTTP tests |
| Error mapping | Exact status, stable code, and safe message test |
| Transactional side effect | Commit and rollback tests |
