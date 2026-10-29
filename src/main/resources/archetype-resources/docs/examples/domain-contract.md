# Example contract: tenant-owned users

This reference demonstrates the [domain contract format](../domain/README.md) using the shipped User sample. It is outside `docs/domain/` because it is not an approved business contract for your application. Its evidence is the sample implementation and tests named below. `make clean-sample` removes that executable sample and its tests; this reference remains illustrative only. Do not infer requirements or current test coverage for a cleaned application from it.

## Terms and context

- **User**: a tenant-owned record with contact details and lifecycle state. Its persisted ID identifies the aggregate; an optional external reference is not a unique identity key.
- **Tenant**: the ownership boundary for repository access. The caller's verified tenant scopes use cases; request data cannot select a different tenant.
- **Authenticated caller**: the application identity and capabilities established by a trusted authentication adapter. The User record's `admin` flag is a deletion-policy fact, not an authentication authority.
- **User management** owns creation, visibility, and lifecycle transitions. The configured token issuer owns authentication; this sample is not an identity provider.

## Confirmed implementation facts and executable constraints

These are facts about the sample, confirmed by code and regression assertions. A product adopting them still owns the business decision.

| ID | Contract | Evidence to locate with `rg` |
| --- | --- | --- |
| USER-01 | A new user belongs to a tenant and starts active. A creation event requires the persisted identity. | `UserTest.createsTenantOwnedUserFromTrustedHash` |
| USER-02 | Only the delete operation enters the deleted state; deleted users cannot change status. | `UserTest.onlyDeleteOperationCanEnterDeletedState` |
| USER-03 | Loading stored state preserves its version and does not create new domain events. | `UserPOConverterTest.roundTripsAllPersistenceFieldsWithoutRaisingEvents` |
| USER-04 | One tenant cannot read another tenant's user. | `UserControllerIntegrationTest.getUserById_CrossTenantNotFound`; `PersistenceIntegrationTest.roundTripsFieldsAndIsolatesTenants` |
| USER-05 | A stale update cannot overwrite a newer stored version. | `PersistenceIntegrationTest.detectsOptimisticLockConflict` |
| USER-06 | Deletion changes lifecycle state without physically deleting the row. | `PersistenceIntegrationTest.softDeleteUsesDomainStatusOnly` |

## State transitions

The implementation defines `ACTIVE`, `INACTIVE`, `LOCKED`, and `DELETED`.

| Operation | Source | Target and guard | Effect |
| --- | --- | --- | --- |
| Create | No user | `ACTIVE`, validated tenant and creation values | Creation event is collected only when the first save supplies an ID |
| Change status | Any non-deleted state | Any non-deleted state; an actual change requires a persisted ID | Actual changes collect a status event; choosing the same state does nothing |
| Delete | Any non-deleted state | `DELETED`; application use case also applies the deletion policy | Collects a status event; later status changes are rejected |

The table describes the aggregate; it does not grant authorization to invoke a use case. `UserDomainServiceImpl.canDeleteUser` additionally rejects administrator records, missing users, and already deleted users. Check the application path when changing that policy.

## Events and delivery

`UserCreatedEvent` states that a new aggregate received its persisted identity; `UserStatusChangedEvent` records an actual lifecycle transition with its old/new states and reason. Both identify the tenant and aggregate. Creation includes username and email, so treat its payload as personal data.

Collection inside an aggregate is distinct from publication. The application registers publication after commit, independently of cache work; rollback publishes neither. `AfterCommitExecutorTest` and the command-template tests cover these generic guarantees and remain after cleanup. Delivery is in-process and not durable across a process failure; cross-service delivery guarantees require an explicit design decision such as an outbox.

## Open assumptions — not requirements

- Should deletion eventually erase personal data, and on what retention schedule? The sample's soft deletion does not answer this.
- Should locking require a mandatory reason or prohibit specific transitions? The sample does not define such a business policy.
- Does another service need durable event delivery or deduplication? Do not infer this from the existence of domain events.

If one of these questions applies to your application, resolve it with its business owner before changing behavior. Record confirmed decisions and tests in the application's own contract under `docs/domain/`, not in this reference example.
