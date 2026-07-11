# Getting started

This guide uses the current `main` branch (`2.1.0-SNAPSHOT`) and JDK 25. Maven Central currently contains only the legacy `1.1.0` Spring Boot 3.5 template, so install the archetype locally before generating a project.

The `v2.0.0` release tag uses JDK 21. To use that revision instead, check out the tag and replace `2.1.0-SNAPSHOT` with `2.0.0` in the generation command.

## Prerequisites

Check the local toolchain:

```bash
java -version
./mvnw -version
docker version
docker compose version
```

Use Maven 3.9 or newer. Projects generated from `main` include Maven Wrapper 3.9.16. Docker is required for the supplied MySQL service and Testcontainers integration tests.

Install the archetype from the current checkout:

```bash
./mvnw clean install -Dgpg.skip=true
```

## Generate a project

```bash
cd ..
./Atom-Archetype/mvnw -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.1.0-SNAPSHOT \
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

Always set `archetypeVersion` to the version installed by the selected checkout.

## Start MySQL

The development datasource matches the generated Compose service:

```bash
cd orders-service
docker compose up -d mysql
docker compose ps
```

MySQL listens on `localhost:3306`. Flyway applies migrations from `infra/persistence/src/main/resources/db/migration` when the application starts.

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

## Run the application

No Spring profile is active by default. Start the application with the `dev` profile and enable the development trusted-header adapter:

```bash
SPRING_PROFILES_ACTIVE=dev \
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
ATOM_REDIS_ENABLED=${ATOM_REDIS_ENABLED:-false} \
sh ./mvnw -f start/pom.xml spring-boot:run
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
    "password": "Password1!",
    "realName": "Alice"
  }'
```

See the generated project's [configuration guide](../src/main/resources/archetype-resources/docs/configuration.md) for production profiles, authentication, and Redis settings. See [Architecture](architecture.md) for module boundaries and [Upgrade guide](upgrade-guide.md) for version differences.
