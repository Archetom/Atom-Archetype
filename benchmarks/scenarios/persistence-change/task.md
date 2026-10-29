# Persist a user's preferred language

Add `preferredLanguage` to the existing User lifecycle without changing module boundaries. These are benchmark-specific confirmed requirements:

- Persist it in `t_user.preferred_language` as `VARCHAR(10) NOT NULL DEFAULT 'en'` through a new Flyway migration. An existing version-1 database containing users must upgrade successfully, preserving data and assigning `en` to old rows. Never edit an applied migration.
- New users default to `en`. Expose `String User.getPreferredLanguage()` and the business method `void User.changePreferredLanguage(String language)`. Reject null, blank, or longer-than-10-character values using `InvalidValueException` without changing the previous value. Nonblank values of length 1–10 are stored verbatim; no normalization or language registry is requested.
- Include the value in PO conversion, the named reconstruction snapshot, mapper-backed list queries, and both existing detail/list HTTP response representations as JSON `preferredLanguage`. Preserve source compatibility of existing snapshot construction, factories, and tests. No new write HTTP endpoint or gRPC field is required.
- A save/read/re-save round trip must retain non-default values. Reconstruction must not raise events or lose version. Tenant-isolated reads and writes and optimistic-lock rejection of stale updates must remain effective.

Add domain/persistence/API tests appropriate to the change, including database migration coverage. Keep the database authoritative, retain Flyway as the only schema initializer, and preserve existing runtime dependencies and verification tools. Report verified and blocked checks. Only edit this workspace. Do not inspect evaluator or benchmark-harness files.
