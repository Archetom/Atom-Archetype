# Getting started

This guide uses stable release `2.3.0` from Maven Central, with JDK 27, Spring Boot 4.1.2, and atom-common 1.2.0. Existing applications should follow the [upgrade guide](upgrade-guide.md) instead of generating over their current source tree.

The older `2.2.0` release uses JDK 25; `2.0.0` uses JDK 21. To use an older version, select its matching JDK and exact archetype version in the generation command.

## Prerequisites

Check the local toolchain:

```bash
java -version
mvn -version
docker version
docker compose version
```

For `2.3.0`, use Maven 3.10.0 or newer for generation, then use the generated project's Maven Wrapper 3.10.0. Docker is required for the supplied MySQL service and Testcontainers integration tests. Java 27 is a non-LTS release and is required for both building and running the generated application.

`main` remains the development branch and uses the same JDK 27/Maven 3.10.0 baseline. Run `CI=false make demo` from the repository root only when you intend to install and generate that checkout instead of the stable release.

## Generate a project

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.3.0 \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

| Property | Purpose |
|---|---|
| `groupId` | Maven group for the generated modules |
| `artifactId` | Root project name and module-name prefix |
| `package` | Base Java package |
| `version` | Initial application version |

Set `archetypeVersion` to the exact release you intend to use.

## Start MySQL

The development datasource matches the generated Compose service:

```bash
cd orders-service
docker compose up -d mysql
docker compose ps
```

MySQL listens on `localhost:3306`. Flyway applies migrations from `infra/persistence/src/main/resources/db/migration` when the application starts.

The generated Compose file pins the MySQL image, and the integration tests use the same image. Back up persistent data and follow MySQL's supported upgrade path before attaching a data volume created by an older MySQL release, or create a fresh volume for disposable development data.

Redis is disabled by default. To enable the Redis adapter locally:

```bash
docker compose up -d redis
export ATOM_REDIS_ENABLED=true
```

## Build and test

```bash
sh ./mvnw clean install
```

Run the MySQL Testcontainers tests with Docker available:

```bash
CI=true sh ./mvnw test
```

Release `2.3.0` also includes `AGENTS.md`, `llms.txt`, five workflows under `.agents/skills/`, and the shared validation entry point:

```bash
make agent-check SCOPE=fast
make agent-check SCOPE=affected
make agent-check                 # Full verification, requiring Docker.
```

`affected` conservatively selects checks from Git changes and falls back to full verification when the impact is uncertain or its optional Python 3 selector is unavailable. Read the generated project's testing guide for the selected scope; a fast pass does not certify Docker integration behavior. The [business-task benchmark](../benchmarks/README.md) is a separate repository-only tool, not part of the generated application.

## Run the application

No Spring profile is active by default. Start the application with the `dev` profile and enable the development trusted-header adapter:

```bash
SPRING_PROFILES_ACTIVE=dev \
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
ATOM_REDIS_ENABLED=${ATOM_REDIS_ENABLED:-false} \
sh ./mvnw -pl start -am spring-boot:run
```

Available development endpoints:

| Endpoint | Access |
|---|---|
| `http://localhost:8080/actuator/health` | Anonymous; details depend on authorization |
| `http://localhost:8080/swagger-ui/index.html` | Anonymous |
| `http://localhost:8080/v3/api-docs` | Anonymous |
| `http://localhost:8080/api/v1/users/**` | Authentication and authority required |

Create a sample user:

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -H 'X-Dev-User-Id: 1001' \
  -H 'X-Dev-Tenant-Id: 42' \
  -d '{
    "username": "alice_01",
    "email": "alice@example.com",
    "password": "correct horse battery staple",
    "realName": "Alice"
  }'
```

See the generated project's [configuration guide](../src/main/resources/archetype-resources/docs/configuration.md) for production profiles, authentication, and Redis settings. See [Architecture](architecture.md) for module boundaries and [Upgrade guide](upgrade-guide.md) for version differences.
