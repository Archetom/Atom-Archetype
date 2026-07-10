# Getting started

This guide generates and runs `io.github.archetom:atom-archetype:2.0.0` from Maven Central. Version `1.1.0` is the legacy Spring Boot 3.5 template and does not match this guide.

## Prerequisites

Verify the local toolchain first:

```bash
java -version
mvn -version
docker version
docker compose version
```

Use JDK 21 and Maven 3.9 or newer. Docker is optional for domain unit tests, but it is required for the supplied MySQL service and Testcontainers integration tests.

## Generate the project

Generate with the exact released version:

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.0.0 \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

The coordinates mean:

| Property | Purpose |
|---|---|
| `groupId` | Maven group for all generated modules |
| `artifactId` | Root project name and module-name prefix |
| `package` | Base Java package; normally a valid reverse-domain name |
| `version` | Initial version of the generated application |

Pin `archetypeVersion`; omitting it makes generation dependent on repository metadata and is not reproducible.

## Start local infrastructure

The default development datasource matches the generated Compose service:

```bash
cd orders-service
docker compose up -d mysql
docker compose ps
```

MySQL runs on `localhost:3306`. Flyway applies the migrations from `infra/persistence/src/main/resources/db/migration` when the application starts. There is no second schema-init mechanism to run manually.

Redis is optional and disabled by default. To exercise the Redis adapter:

```bash
docker compose up -d redis
export ATOM_REDIS_ENABLED=true
```

With Redis disabled, the generated no-op cache adapter is used and business correctness is unchanged.

## Build and test

```bash
sh ./mvnw clean install
```

Domain and application unit tests run normally. Integration tests are intentionally explicit because they start containers:

```bash
CI=true sh ./mvnw test
```

Make sure Docker is running before enabling the integration tests.

## Run the application

No Spring profile is activated implicitly. Select `dev` yourself and explicitly enable the development-only trusted-header adapter:

```bash
SPRING_PROFILES_ACTIVE=dev \
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
ATOM_REDIS_ENABLED=${ATOM_REDIS_ENABLED:-false} \
sh ./mvnw -f start/pom.xml spring-boot:run
```

Useful endpoints:

| Endpoint | Access |
|---|---|
| `http://localhost:8080/actuator/health` | Anonymous; details depend on authorization |
| `http://localhost:8080/swagger-ui/index.html` | Anonymous |
| `http://localhost:8080/v3/api-docs` | Anonymous |
| `http://localhost:8080/api/v1/users/**` | Authenticated and authority-checked |

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

The identity headers do not carry authorities. The server obtains the development authorities from `atom.security.trusted-header.authorities`.

## Production configuration

The production profile requires database credentials and never accepts the development identity headers:

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL='jdbc:mysql://db.example:3306/orders'
export SPRING_DATASOURCE_USERNAME='orders_app'
export SPRING_DATASOURCE_PASSWORD='replace-with-a-secret-source'
```

Before exposing a business endpoint, integrate a real authentication mechanism with Spring Security. The adapter must:

1. verify the external credential using the selected IdP or trust mechanism;
2. create a trusted principal containing a positive actor ID and tenant ID;
3. map verified authorities to the generated `AuthenticatedCaller`;
4. keep client-controlled request bodies and role headers out of that mapping.

The template does not choose an IdP. You can configure OAuth 2.0 resource server/JWT, mTLS, a gateway-integrated principal, or another Spring Security mechanism while retaining the same application contract.

For production Redis, set `ATOM_REDIS_ENABLED=true` only after supplying and testing the Spring Data Redis connection configuration. Leave it false when caching is not required.

## Next steps

- Read the [architecture rules](architecture.md) before adding a bounded context.
- Apply the [naming conventions](naming-conventions.md) to new types.
- Review [security and compatibility changes](upgrade-guide.md) before merging a newer template into an existing project.
- Use [troubleshooting](troubleshooting.md) when generation, startup, authentication, Flyway, or Testcontainers fails.
