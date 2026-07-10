# Testing guide

Tests follow the architecture: domain behavior is tested without Spring, application orchestration uses test doubles, adapters are tested at their boundary, and a small MySQL Testcontainers suite verifies the assembled application.

## Commands

Run all fast tests:

```bash
sh ./mvnw test
```

Run one module while building its dependencies:

```bash
sh ./mvnw -pl domain -am test
sh ./mvnw -pl application -am test
sh ./mvnw -pl infra/persistence -am test
sh ./mvnw -pl infra/rest -am test
```

Run the Docker-backed integration suite:

```bash
CI=true sh ./mvnw test
```

The `CI=true` switch enables the generated integration test classes. Docker must be available. Testcontainers starts MySQL 8.4.10; Redis remains disabled because cache correctness is covered through its port and no-op adapter.

## Test layers

| Layer | Test focus | Typical dependencies |
| --- | --- | --- |
| Domain | Value validation, aggregate transitions, policies, events | JUnit and assertions only |
| Application | Authority checks, tenant propagation, orchestration, after-commit scheduling | Mocks or small fakes for ports |
| Persistence converter | Full PO/aggregate round trip including version and timestamps | MapStruct mapper instance |
| REST | Error code/status mapping, authentication rejection, safe messages | Spring MVC/Security test support |
| Start integration | Flyway, MySQL queries, tenant isolation, optimistic locking, complete HTTP flow | Spring Boot, MockMvc, Testcontainers |
| Architecture | Forbidden imports across domain, API, shared, application, and infrastructure boundaries | JUnit source inspection and Maven Enforcer |

Prefer the lowest layer that can prove the behavior.

## Domain tests

Create aggregates through factories or public creation methods and invoke behavior methods directly:

```java
@Test
void delete_active_user_changes_status_and_records_event() {
    User user = createUserForTenant(1L);

    user.delete();

    assertTrue(user.isDeleted());
    assertFalse(user.getDomainEvents().isEmpty());
}
```

Domain tests should not use Spring annotations, databases, HTTP, or mocks of domain objects. Test valid transitions, invalid transitions, boundary values, equality, and event payloads.

Reconstitution tests must verify that loading persisted state restores `version` and does not raise new events.

## Application tests

Mock output ports at the application boundary, not internal methods. Important cases include:

- null or malformed caller is rejected before repository access;
- missing authority is rejected;
- `TenantId` derived from the caller is passed to every repository/cache call;
- cache keys differ across tenants;
- a cache miss queries the repository;
- a command failure marks the active transaction rollback-only;
- after-commit actions do not run on rollback;
- an event list is captured before the aggregate clears it.

Use `AfterCommitExecutorTest` as the pattern for commit and no-transaction behavior. Do not assert only that a method was invoked; assert timing relative to transaction completion.

## Persistence tests

`UserPOConverterTest` should cover every persisted field:

- ID and tenant;
- username, email, and phone;
- password hash and real name, without exposing the hash through `toString`;
- status and external flags;
- optimistic-lock version;
- created and updated timestamps;
- reconstruction with no domain events.

Whenever `UserPO`, mapper XML, or a migration changes, extend this round-trip test.

## Integration tests

`BaseIntegrationTest` provides:

- Spring Boot with the `test` profile;
- MySQL Testcontainers properties;
- Flyway schema initialization;
- MockMvc helpers that add test actor and tenant headers;
- explicit database cleanup.

Integration tests intentionally do not wrap the whole test in a rollback transaction. Real commits are required to verify `AfterCommitExecutor`, cache/event timing, and optimistic locking.

The generated suite should retain these scenarios:

1. Flyway creates `t_user` and records migration history.
2. A PO round trip retains every mapped column.
3. Two copies of one aggregate cannot both update from the same version.
4. A user in tenant A is invisible to tenant B.
5. Cache lookups are tenant scoped.
6. Soft deletion persists `status=DELETED` and does not physically remove the row.
7. Anonymous API calls return 401.
8. Invalid request data returns 400.
9. Missing resources return 404 and version conflicts return 409.
10. Internal failures do not expose exception messages.
11. A caller with `users:write` cannot delete through the status endpoint.
12. Concurrent unique-key conflicts return 409 rather than 500.
13. Redis-disabled health remains UP without a Redis server.
14. `/v3/api-docs` publishes the generated project name and version.

## Authentication tests

The test profile explicitly enables the trusted-header adapter. Tests use positive numeric values for both:

```text
X-Dev-User-Id
X-Dev-Tenant-Id
```

Add negative cases for:

- neither header present;
- only one header present;
- zero, negative, or non-numeric values;
- missing authority;
- attempted authority or administrator escalation through a header;
- trusted-header property combined with the `prod` profile.

Do not treat the development adapter as a substitute for testing the production authentication integration.

## Reliable test practices

- Use unique values or explicit cleanup; do not depend on execution order.
- Avoid `Thread.sleep`; use synchronization primitives, transaction hooks, or Awaitility-style polling when asynchronous behavior is intentional.
- Do not use a second schema initializer. Flyway owns schema creation in every environment.
- Do not mock value objects or aggregates.
- Assert tenant and version predicates, not only returned data.
- Keep passwords and tokens out of assertion failure messages and captured logs.
- Keep Docker images pinned to a supported version.

## Change-to-test map

| Change | Minimum verification |
| --- | --- |
| Aggregate or value object | Domain unit test |
| Application command/query | Application test with caller, tenant, success, and failure |
| Cache behavior | Tenant-key and fallback tests |
| PO or converter | Complete round-trip test |
| Mapper query | MySQL integration test |
| Flyway migration | Empty-database migration integration test |
| Security route | Anonymous, allowed, and forbidden HTTP tests |
| Error mapping | Exact status, stable code, and safe message test |
| Transactional side effect | Commit and rollback tests |

Before release, both `sh ./mvnw test` and `CI=true sh ./mvnw test` must pass from a freshly generated project.
