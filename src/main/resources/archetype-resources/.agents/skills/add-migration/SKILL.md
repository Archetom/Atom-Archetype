---
name: add-migration
description: Evolve the generated application's Flyway schema and corresponding persistence mappings with tenant, optimistic-lock, and data-compatibility checks.
---

# Add a migration

Use [application invariants](../../../AGENTS.md#domain-persistence-and-transactions), the [migration workflow](../../../docs/usage-guide.md#add-a-database-migration), and [persistence testing](../../../docs/test-guide.md#persistence).

1. Inspect existing migrations under `infra/persistence/src/main/resources/db/migration`, their versions, and the affected stored data. Add the next unused `V...__description.sql`; never rewrite an applied migration. Flyway remains the only schema source.
2. State the old/new schema and data assumptions. Assess existing-row backfill, nullable-to-required changes, indexes, tenant-qualified uniqueness, and whether old/new application versions must coexist. Use an additive migration followed by a later removal when compatibility needs it; do not infer deployment or permission to delete data.
3. Update PO, mapper SQL/result mappings, converter, and aggregate reconstruction consistently. A persisted field belongs in the aggregate only when it is domain state. Keep explicit tenant predicates, fail-closed missing scope, stored version restoration, and optimistic compare-and-increment behavior.
4. Extend converter round-trip tests for all affected fields and reconstruction without events. Add MySQL Testcontainers assertions for migration from an empty database; for a data transformation, also exercise representative pre-migration rows and verify the upgraded values. Test cross-tenant and stale-version failures when those paths change.
5. Run focused persistence tests first, then `make agent-check SCOPE=full` from a generated project. In the archetype repository, run its root entry point so generation and cleanup are also checked. Docker unavailability leaves this acceptance criterion unverified.

Acceptance: a fresh schema and affected upgrade path work; mappings agree with stored data; existing tenant and concurrency guarantees hold; required MySQL tests have passed. Record deployment compatibility assumptions and any unverified migration path.
