---
name: add-api
description: Add or change an HTTP endpoint in this application, including public contracts, authorization, mapping, OpenAPI, and API regression coverage.
---

# Add an HTTP API

Read the relevant [application rules](../../../AGENTS.md), [object layering](../../../docs/object-layering.md), and [HTTP workflow](../../../docs/usage-guide.md#add-an-http-endpoint). Commands run from the generated project root; archetype maintainers use the repository root guide for generation checks.

1. Define the method, `/api/v1/` route, request/response fields, validation, and stable error/status outcomes. Keep published types in `api`; do not expose domain entities, POs, application VOs, or identity parameters.
2. Reuse or extend the facade/use case. The facade resolves a verified caller; the use case checks authority through `CallerGuard` and derives tenant scope from that caller. An ordinary request cannot select actor, tenant, roles, or administrator privileges.
3. Declare route authorities in the API area's `ApiRouteAuthorization` bean using the same constants as the use case. Add capabilities to dev/test configuration only if required; never change the production trusted-header guard to make a test pass.
4. Keep controller work to binding, validation, facade delegation, mapping, and HTTP results. Add or update the boundary assembler and OpenAPI descriptions, including required fields and error responses. Avoid secrets in logs, exceptions, and generated examples.
5. Cover valid and invalid requests, unauthenticated `401`, authenticated-but-forbidden `403`, cross-tenant visibility, response shape, and safe stable errors. Use REST tests for transport behavior and the assembled application tests for actual authentication/tenant routing. Verify OpenAPI when the published schema changes.
6. Run affected tests and the required scopes from [verify-change](../verify-change/SKILL.md); authentication, authorization, and tenant behavior require the Docker suite.

Acceptance: HTTP and use-case authorization agree; callers cannot supply trusted identity; response and OpenAPI contracts match; regression tests exercise the real boundary and forbidden cases.
