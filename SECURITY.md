# Security policy

## Supported versions

Security fixes target the upcoming 2.0 line on `main`. Maven Central 1.1.0 is a legacy architecture and should be assessed independently before production use.

## Reporting a vulnerability

Please use a [private GitHub security advisory](https://github.com/Archetom/atom-archetype/security/advisories/new). Do not open a public issue for an unpatched vulnerability.

Include the affected archetype version or commit, generated code path, impact, reproduction steps, and any suggested mitigation. Avoid including real credentials, personal data, or production database content.

## Generated projects

Generation transfers the source code to the adopting team. Consumers remain responsible for dependency updates, production identity-provider integration, secret storage, authorization policy, network controls, database backups, and monitoring.
