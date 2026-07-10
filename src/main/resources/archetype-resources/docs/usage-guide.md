# Development workflow

This guide describes the shortest safe path for extending the generated application. Read [architecture](architecture.md) first; its dependency and security invariants are mandatory.

## Local development loop

Start MySQL:

```bash
docker compose up -d mysql
```

Build and run with local trusted authentication explicitly enabled:

```bash
sh ./mvnw clean install

ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Use both development identity headers for protected endpoints:

```bash
curl \
  -H 'X-Dev-User-Id: 1' \
  -H 'X-Dev-Tenant-Id: 1' \
  http://localhost:8080/api/v1/users
```

Stop local services without deleting data:

```bash
docker compose down
```

## Add a use case

Work from the inside outward.

1. Define or update domain vocabulary: value objects, enum, aggregate behavior, policy, and domain error.
2. Add a domain repository or service port only when the domain needs an external capability.
3. Add the application use case with explicit `AuthenticatedCaller` and authority checks.
4. Use `CommandServiceTemplate` for state changes or `QueryServiceTemplate` for reads.
5. Implement persistence, messaging, or external adapters in `infra`.
6. Add public Request/Response and facade methods in `api`.
7. Bind HTTP and authentication details in `infra/rest`.
8. Wire runtime adapters through Spring configuration and `start`.
9. Add tests at the lowest useful layer, then an integration test for the boundary.

Do not begin by creating a database table or controller. First name the business behavior and ownership rule.

## Implement a command

A command is transactional and uses `CommandServiceTemplate`:

```java
@Transactional
public Result<Void> updateUserStatus(
        AuthenticatedCaller caller,
        Long rawUserId,
        String rawStatus) {

    return commandTemplate.execute(
            UseCaseOperation.USER_STATUS_UPDATE,
            new ServiceOperation<Void>() {
                private TenantId tenantId;
                private UserId userId;
                private User user;
                private UserStatus status;

                @Override
                public void validate() {
                    tenantId = requireCaller(caller, "users:write");
                    userId = requireUserId(rawUserId);
                    status = requireUserStatus(rawStatus);
                }

                @Override
                public void prepare() {
                    user = userRepository.findById(tenantId, userId)
                            .orElseThrow(() -> new UserNotFoundException(rawUserId));
                }

                @Override
                public Void execute() {
                    user.changeStatus(status, "requested by actor");
                    user = userRepository.save(tenantId, user);
                    return null;
                }

                @Override
                public void onSuccess(Void ignored) {
                    var events = user.pullDomainEvents();
                    afterCommitExecutor.execute(() -> {
                        userCacheService.evictUser(tenantId, userId);
                        domainEventPublisher.publishAll(events);
                    });
                }
            });
}
```

Important properties:

- authorization and tenant derivation happen before data access;
- the repository receives tenant explicitly;
- aggregate methods perform the state transition;
- the repository enforces optimistic locking;
- cache eviction and event publication occur after commit;
- the template preserves rollback when a failure is converted into a result.

Do not send an email, publish to a broker, or mutate Redis before the database transaction commits. Use a transactional outbox when delivery must be durable.

## Implement a query

Queries use `QueryServiceTemplate` and still require explicit caller and tenant context:

```java
public Result<UserVO> getUserById(AuthenticatedCaller caller, Long rawUserId) {
    return queryTemplate.execute(UseCaseOperation.USER_GET, new ServiceOperation<UserVO>() {
        private TenantId tenantId;
        private UserId userId;

        @Override
        public void validate() {
            tenantId = requireCaller(caller, "users:read");
            userId = requireUserId(rawUserId);
        }

        @Override
        public UserVO execute() {
            UserVO cached = userCacheService.getCachedUser(tenantId, userId);
            if (cached != null) {
                return cached;
            }

            User user = userRepository.findById(tenantId, userId)
                    .orElseThrow(() -> new UserNotFoundException(rawUserId));
            return UserAssembler.toVO(user);
        }
    });
}
```

Tenant identity must be part of every cache key. A cache hit must never allow a caller to bypass ownership checks.

## Add aggregate behavior

Prefer intention-revealing behavior:

```java
user.changeEmail(new Email(address));
user.changeStatus(UserStatus.LOCKED, reason);
user.delete();
```

Avoid:

```java
user.setStatus("LOCKED");
user.setDeleted(true);
```

Factories validate new state. `reconstitute` restores persisted state without generating events. If a new database-generated ID is needed by a creation event, register the event only after persistence has assigned that ID.

## Add a repository operation

Define the port in `domain` with tenant as a required argument:

```java
Optional<User> findByEmail(TenantId tenantId, String email);
```

Implement it in `infra/persistence` with a tenant predicate:

```java
LambdaQueryWrapper<UserPO> query = new LambdaQueryWrapper<>();
query.eq(UserPO::getTenantId, tenantId.getValue())
     .eq(UserPO::getEmail, email);
```

Fail when tenant is absent. Never interpret null tenant as an administrator or unscoped query. Cross-tenant administrative use cases require a separate, explicit port and authority.

For aggregate updates, keep the loaded version unchanged until MyBatis-Plus performs the compare-and-increment. Treat zero updated rows as an `AggregateVersionConflictException`.

## Change the database

Create the next migration under:

```text
infra/persistence/src/main/resources/db/migration
```

For example:

```text
V2__add_user_last_login_time.sql
```

In the same change, update:

- the migration;
- the PO and annotations;
- converter mappings in both directions;
- mapper XML result maps and column lists;
- aggregate state and `reconstitute` when the field is domain state;
- unit and integration tests;
- relevant documentation.

Do not add `schema.sql`, Docker init SQL, or a second test schema.

## Add an HTTP endpoint

The REST adapter should do only transport work:

1. bind and validate Request data;
2. receive Spring Security `Authentication`;
3. use `AuthenticatedCallerMapper`;
4. call the facade;
5. convert the result to an HTTP response.

Never accept authorities, administrator flags, actor ID, or tenant ID from a normal request body. Do not log complete request objects when they may contain passwords or tokens.

Add route authorization in `SecurityConfig`, then repeat capability authorization in the application use case so non-HTTP adapters remain protected.

## Errors and HTTP behavior

Use the narrowest failure type:

- domain rule failures extend `DomainException` with a stable domain error;
- workflow failures use `ApplicationException`;
- stable non-retryable rejections use `NonRetryableApplicationException`;
- optimistic concurrency uses `AggregateVersionConflictException`;
- unexpected exceptions are logged internally and mapped to a generic public error.

Public mappings include:

| Failure | HTTP status |
| --- | --- |
| Invalid request | 400 |
| Missing authentication | 401 |
| Insufficient authority | 403 |
| Missing resource | 404 |
| Duplicate or version conflict | 409 |
| Other domain rejection | 422 |
| Unexpected internal failure | 500 |

Do not expose stack traces, SQL, credentials, internal class names, or arbitrary exception messages.

## Before opening a change

```bash
sh ./mvnw test
CI=true sh ./mvnw test
```

Also verify:

- a caller from another tenant cannot read, update, delete, or receive a cached object;
- a missing tenant fails closed;
- stale updates return a conflict;
- rollback does not publish events or mutate cache;
- production profile cannot activate trusted headers;
- Flyway starts successfully against an empty MySQL database;
- documentation and `llms.txt` still point to the correct concepts.
