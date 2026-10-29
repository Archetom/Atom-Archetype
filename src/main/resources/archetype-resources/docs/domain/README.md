# Domain contracts

Record your application's business meaning here when it helps explain a feature. One short file per business area is enough; simple CRUD does not need a separate glossary, context map, or state machine. Split these out only when the content grows. A [worked example](../examples/domain-contract.md) is kept separately under `docs/examples/` for reference; it is not a business contract for your application.

## Contract format

Start with the business area, its owner or decision source, and the use cases it serves. Include only relevant sections:

| Section | What to capture |
| --- | --- |
| Terms and relationships | Business meanings, identities, ownership, and relationships; avoid copying field lists |
| Context boundary | Decisions owned here, external responsibilities, and information exchanged |
| Confirmed facts | A decision source or existing behavior, clearly identified as such |
| Open assumptions | The question, its impact, and who can resolve it; never silently promote it to a rule |
| Invariants | Stable IDs, prohibited outcomes, enforcement point, and an executable test reference |
| State transitions | Allowed operation, source/target state, guards, and observable effect |
| Events | The fact represented, when it exists, its consumer, delivery guarantees, and sensitive data |

Use a test class and method name as a durable search reference because Java paths depend on the generated package. Separate behavior observed in code from business facts confirmed by a product owner. A test proves current behavior, not that a new business assumption is correct.

## Change discipline

When a feature changes a recorded contract, update its business description and test together. Confirm unresolved business choices before implementing them. For each invariant, exercise the rejected outcome as well as the valid case at the lowest useful test layer; use MySQL integration tests for persistence claims. The [testing guide](../test-guide.md) maps behavior to test layers.

Review a contract only when the changed feature touches its business area. Keep facts, assumptions, and test references current in the same review as the code. Remove obsolete statements rather than accumulating conflicting versions. `make clean-sample` retains this generic guide and the separate reference example, leaving `docs/domain/` ready for your own contracts. Do not treat reference examples as confirmed requirements or evidence that removed tests still run.
