---
name: create-feature
description: Implement or change a business use case in this generated DDD application, from domain behavior through only the adapters the feature needs.
---

# Create a feature

Use [AGENTS.md](../../../AGENTS.md) for mandatory boundaries and [development workflow](../../../docs/usage-guide.md) for existing extension points. In the archetype source tree, edit these templates but run the repository root's verification commands.

1. Identify the actor, verified tenant, use-case outcome, failure cases, and business facts. Consult relevant [domain contracts](../../../docs/domain/README.md); resolve assumptions that change behavior before enforcing them.
2. Map only the necessary changes: `domain` for business state/rules and repository ports; `application` for orchestration and output ports; `api` for public contracts; the relevant `infra` adapter for transport or persistence; `start` for composition. Reuse existing behavior for simple field updates. Do not generate every DDD object or a new abstraction by default.
3. Implement aggregate behavior and its rejected cases first where domain state changes. Add ports only for current use-case needs. Keep caller/tenant checks in the application path and preserve command/query transaction and after-commit semantics.
4. Add consumed API fields and adapter mappings together. Update migration, PO, reconstruction, and converter only if persisted state changes. Use the [API](../add-api/SKILL.md) or [migration](../add-migration/SKILL.md) workflow when relevant.
5. Test the changed behavior at the lowest useful layer, including unauthorized callers, tenant scope, and rollback/conflicts where affected. Update an existing domain contract when its meaning changes; do not create documentation for trivial facts already obvious from code.
6. Follow [verify-change](../verify-change/SKILL.md), beginning with affected module tests, then the required broader checks.

Acceptance: the requested use case works through its actual entry point; invalid cases are covered; no unused object, port, or conversion was introduced; required checks pass or are reported as unverified with their cause.
