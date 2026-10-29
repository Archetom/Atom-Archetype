# Enforce the locked-user review transition

The generated example already prevents reactivation of a deleted user. Implement this additional, benchmark-specific confirmed rule in the existing User aggregate:

> A LOCKED user cannot transition directly to ACTIVE. Review must first move it to INACTIVE; INACTIVE may then transition to ACTIVE.

Apply the rule to both newly created and reconstituted aggregates through `User.changeStatus`. A prohibited transition throws the existing `UserDomainException` before changing status, audit time, version, or pending events. A restored LOCKED user must follow the same rule regardless of its version.

Preserve all other behavior: ACTIVE → LOCKED remains legal, LOCKED → INACTIVE → ACTIVE raises the existing status events, changing to the current status remains a no-op, and only `delete()` may enter DELETED. Deleted users remain terminal. Existing HTTP behavior should continue to map domain rejection through its existing error handling.

Add focused regression tests. Keep the current domain model and factory/reconstruction contracts; no new framework or state-machine abstraction is needed. Do not modify verification tooling or weaken existing tests. Report checks and any blocked validation. Only edit this workspace. Do not inspect evaluator or benchmark-harness files.
