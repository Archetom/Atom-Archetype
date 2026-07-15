# Testing guide

Test domain behavior without Spring, application orchestration through ports, adapters at their boundaries, and the assembled application against MySQL.

## Commands

Run fast tests:

```bash
sh ./mvnw test
```

Run one module and its dependencies:

```bash
sh ./mvnw -pl shared test
sh ./mvnw -pl domain -am test
sh ./mvnw -pl application -am test
sh ./mvnw -pl infra/persistence -am test
sh ./mvnw -pl infra/rest -am test
```

Run Docker-backed integration tests:

```bash
CI=true sh ./mvnw test
```

`CI=true` enables the generated integration test classes. Docker must be available. Testcontainers starts MySQL 9.7.1 LTS; Redis remains disabled.

## Test layers

| Layer | Test focus | Typical dependencies |
| --- | --- | --- |
| Shared | Framework-neutral result, error, and pagination utilities | JUnit and assertions only |
| Domain | Value validation, aggregate transitions, policies, events | JUnit and assertions only |
| Application | Authority checks, tenant propagation, orchestration, post-commit scheduling | Mocks or small port fakes |
| Persistence converter | Full PO/aggregate round trip, version, timestamps | MapStruct mapper instance |
| REST | Error/status mapping, authentication rejection, safe messages | Spring MVC and Security test support |
| Start integration | Flyway, MySQL, tenant isolation, locking, HTTP flow | Spring Boot, MockMvc, Testcontainers |
| Architecture | Forbidden dependencies, adapter bypasses, entity setters, naming | ArchUnit bytecode inspection and Maven Enforcer |

Use the lowest layer that proves the behavior.

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

For commands, verify that `validate` and `prepare` precede the independent transaction, and that a failure rolls back before it is converted to a `Result`. Post-commit work runs after commit and does not run after rollback; register event and cache callbacks independently. `CommandServiceTemplateTest` and `AfterCommitExecutorTest` are the references.

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

### Trusted-header authentication

The `test` profile enables the trusted-header adapter. Requests use positive numeric `X-Dev-User-Id` and `X-Dev-Tenant-Id` values.

Cover missing, incomplete, and invalid header pairs; missing authority; attempted authority or administrator escalation; and trusted headers combined with `prod`. Keep credentials out of assertion output and captured logs. These tests do not replace tests for the production authentication integration.

## Change-to-test map

| Change | Minimum verification |
| --- | --- |
| Aggregate or value object | Domain unit test |
| Application command/query | Caller, tenant, success, and failure application tests |
| Cache behavior | Tenant-key and database fallback tests |
| PO or converter | Complete round-trip test |
| Mapper query | MySQL integration test |
| Flyway migration | Empty-database migration test |
| Security route | Anonymous, allowed, and forbidden HTTP tests |
| Error mapping | Exact status, stable code, and safe message test |
| Transactional side effect | Commit and rollback tests |
