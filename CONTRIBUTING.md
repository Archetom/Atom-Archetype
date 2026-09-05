# Contributing

Thank you for improving Atom Archetype. Use [AGENTS.md](AGENTS.md) for editing and verification rules; consult [docs/architecture.md](docs/architecture.md) when changing application boundaries.

## Workflow

1. Open an issue for broad architecture, security, module, or generated-contract changes.
2. Keep commits focused and preserve unrelated local work.
3. Update code, tests, documentation, archetype metadata, and `clean.sh` together when their contracts change.
4. Never weaken tenant or authentication invariants to preserve source compatibility.
5. Describe breaking changes in [CHANGELOG.md](CHANGELOG.md) and [docs/upgrade-guide.md](docs/upgrade-guide.md).

## Verification

Follow the change-specific checks in [AGENTS.md](AGENTS.md#verification). `make demo` includes installation and generates `target/generated-projects/atom-demo` by default; run application Maven commands from that generated project root.

Pull requests should state which commands passed and whether Docker-backed tests were run.

## Style

- Follow [docs/naming-conventions.md](docs/naming-conventions.md).
- Keep the domain independent of Spring, API, and infrastructure modules.
- Pass caller and tenant context explicitly.
- Add database changes only as new Flyway migrations.
- Do not commit secrets, credentials, generated logs, build output, or local configuration.
