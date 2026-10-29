---
name: review-architecture
description: Review a proposed change for concrete DDD boundary, security, persistence, and transaction defects or unnecessary abstractions in this application.
---

# Review architecture

Read the change and relevant [application rules](../../../AGENTS.md), then load only the affected sections of [architecture](../../../docs/architecture.md) or [object layering](../../../docs/object-layering.md). Review existing patterns before proposing a new one.

1. Trace dependencies through the changed call path: framework-neutral domain and API/shared, application use cases, adapter implementations, and composition in `start`. Check controller/repository bypasses and domain/PO/VO leakage in public signatures, including nested generic payloads.
2. Check aggregate factories, reconstruction, mutation methods, and invariants against tests. Distinguish intentional persistence synchronization from setters that bypass business rules.
3. Trace authenticated caller and tenant from entry point to every repository/cache operation. Inspect route and use-case capabilities, fail-closed paths, tenant predicates/cache keys, and unexpected-error handling.
4. Trace transactions, stale-version updates, and commit/rollback effects. Confirm cache and event registrations remain independent. Static dependency checks do not establish SQL tenant isolation, JWT validity, or runtime commit ordering; locate the relevant JUnit/integration evidence in the [testing guide](../../../docs/test-guide.md).
5. Challenge new interfaces, DTOs, adapters, mappings, and inheritance only when they add measurable complexity without a current use case. Do not reorganize the architecture for style.
6. Run `make agent-check SCOPE=fast` for static/module changes and the full Docker scope for affected security, persistence, transaction, cache, or event behavior. For template changes, use the archetype root's generation checks as well.

Output actionable findings with severity, file and line, triggering scenario, impact, and a concrete repair direction. Separate confirmed defects from questions and unverified risks. If no defect is found, say so and list the checks and remaining coverage limits. Do not treat a passing ArchUnit suite as proof of runtime security.
