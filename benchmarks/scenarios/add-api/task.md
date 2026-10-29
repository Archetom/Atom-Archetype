# Add a tenant-scoped email update API

Implement `PUT /api/v1/users/{userId}/email` with JSON body `{"email":"new@example.com"}` in this disposable generated application. This benchmark's confirmed requirement is to update a visible user's email inside the authenticated caller's tenant.

Public contract:

- Return HTTP 200 on success. A subsequent GET and a fresh database read must show the new email.
- Reject missing, null, blank, malformed, or over-254-character email with 400 and no mutation. Reuse the domain email rules.
- Require a verified caller and `users:write`: anonymous/invalid tokens return 401, callers with only `users:read` return 403. The Application use case must also enforce these checks when called directly.
- Derive tenant only from verified authentication. A user in another tenant, a missing user, or a deleted user returns 404 and cannot change. Request parameters or headers must not override a bearer token's tenant.
- A duplicate email in the same tenant returns 409 without changing the existing row; the same email in another tenant remains legal.
- Preserve the existing Controller → Facade → Application → Domain/Repository boundaries, optimistic locking and after-commit cache invalidation. Do not expose caller identity in the public facade.
- Expose `Result<Void> updateUserEmail(AuthenticatedCaller caller, Long userId, String email)` on `UserService`, and `Result<Void> updateUserEmail(Long userId, String email)` on `UserFacade`. The HTTP request DTO is your design choice. No gRPC method is required.

Add meaningful tests and OpenAPI documentation. Preserve existing behavior and tests; do not change dependencies, applied migrations, or verification tooling. Report what you verified and anything blocked. Only edit this workspace. Do not inspect evaluator or benchmark-harness files.
