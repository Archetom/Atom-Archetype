# Troubleshooting

## Maven cannot find the 2.x archetype

**Check:** Maven Central currently contains only `1.1.0`. Run `./mvnw clean install -Dgpg.skip=true` from the selected checkout, then generate with its exact version. For current `main`:

```bash
cd ..
./Atom-Archetype/mvnw -U -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.1.0-SNAPSHOT \
  -DgroupId=com.example.demo \
  -DartifactId=demo-service \
  -Dpackage=com.example.demo \
  -Dversion=1.0.0-SNAPSHOT
```

**Fix:** If the installed artifact is still unavailable, inspect `~/.m2/settings.xml` for mirrors, proxies, repository overrides, or offline mode.

## The generated project uses the wrong Java version

**Check:** Maven may use a different JDK from the shell. `v2.0.0` targets JDK 21; current `main` requires JDK 25.

```bash
java -version
./mvnw -version
```

**Fix:** Set `JAVA_HOME` to the required JDK and restart the terminal and IDE.

## The application has no active profile

**Check:** No profile is selected by default.

**Fix:** Select one when starting the application:

```bash
SPRING_PROFILES_ACTIVE=dev sh ./mvnw -f start/pom.xml spring-boot:run
```

For `prod`, also set all three required datasource environment variables. Missing production credentials stop startup.

## API requests return 401 Unauthorized

**Check:** Local trusted-header authentication requires both settings and both request headers:

```bash
SPRING_PROFILES_ACTIVE=dev
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true
```

```text
X-Dev-User-Id: 1001
X-Dev-Tenant-Id: 42
```

Both IDs must be positive integers. `X-User-Id`, `X-Tenant-Id`, and `X-Admin` are ignored.

**Fix:** Enable the adapter only for local `dev` or `test` use. Under `prod`, configure Spring Security and map the verified principal to `AuthenticatedCaller`; trusted headers remain unavailable.

## API requests return 403 Forbidden

**Check:** The authenticated caller needs the authority for the operation:

| Operation | Authority |
|---|---|
| Read/list users | `users:read` |
| Create/update users | `users:write` |
| Delete users | `users:delete` |

**Fix:** For dev/test, update `atom.security.trusted-header.authorities`. For production, check the IdP claim-to-authority mapping. Authorities are not accepted from request headers.

## A user exists but cannot be found

**Check:** Compare the tenant used for the write and read. Repository queries and cache keys are tenant-scoped.

**Fix:** Correct the caller-to-tenant mapping. Cross-tenant administration requires a separate authorized use case; do not remove the tenant predicate.

## MySQL connection fails

**Check:** The development profile expects MySQL on `localhost:3306` with the credentials in `conf/application-dev.yml`. Inspect the generated service:

```bash
docker compose up -d mysql
docker compose ps
docker compose logs mysql
```

**Fix:** If port 3306 is occupied, stop the conflicting service or update both the Compose port mapping and JDBC URL.

## Flyway migration fails

**Check:** Inspect application logs and `flyway_schema_history`. Common causes are an unmanaged schema, an edited migration checksum, insufficient DDL permissions, or existing data that violates a new constraint.

**Fix:** Add a forward migration or create a deliberate baseline for an existing database. Do not add `schema.sql`. Run Flyway repair only after confirming why the recorded state is wrong.

## Redis connection errors

**Check:** Redis is disabled by default. Confirm the setting:

```bash
ATOM_REDIS_ENABLED=false
```

If Redis is enabled, inspect the service and Spring Data Redis settings:

```bash
docker compose up -d redis
docker compose logs redis
```

**Fix:** Leave Redis disabled when caching is not required, or correct its connection settings. Authorization, uniqueness, and aggregate updates must continue to use database-backed checks.

## An update returns a conflict

**Check:** A zero-row update indicates that another transaction changed the aggregate version.

**Fix:** Reload the aggregate and re-evaluate the command. Retry only when the complete use case is safe to repeat; do not overwrite the version.

## Integration tests are skipped

**Check:** Container-backed tests require `CI=true` and a running Docker daemon. Unit tests run without `CI=true`.

**Fix:** Run:

```bash
CI=true sh ./mvnw test
```

## Swagger UI is unavailable

**Check:** Confirm that the application is running and `start` includes `infra/rest`. The development URLs are:

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

**Fix:** The `prod` profile disables both endpoints. Enable them only with an appropriate access policy.

## Maven reports dependency convergence or missing Boot 4 classes

**Check:** Inspect the effective dependency graph:

```bash
sh ./mvnw dependency:tree
sh ./mvnw help:effective-pom
```

**Fix:** Use the Spring Boot BOM instead of pinning individual Spring artifacts. Upgrade the parent, Boot starters, MyBatis-Plus starter, SpringDoc, Jackson APIs, and test dependencies as one compatible set.

## Velocity placeholders are wrong in a generated file

**Check:** `${package}` and `${artifactId}` are expected template variables under `src/main/resources/archetype-resources`.

For a literal Spring placeholder in filtered YAML, use:

```velocity
#set( $dollar = '$' )
password: ${dollar}{SPRING_DATASOURCE_PASSWORD}
```

**Fix:** Rebuild and verify the generated project. Update `archetype-metadata.xml` for missing generated files and `clean.sh` for renamed or removed sample files.

```bash
make install
make demo
cd ~/Downloads/atom-demo
sh ./mvnw compile
CI=true sh ./mvnw test
```
