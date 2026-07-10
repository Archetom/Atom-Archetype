# Troubleshooting

## Maven cannot find the archetype

Refresh Maven metadata and generate with the exact stable version:

```bash
mvn -U -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.0.0 \
  -DgroupId=com.example.demo \
  -DartifactId=demo-service \
  -Dpackage=com.example.demo \
  -Dversion=1.0.0-SNAPSHOT
```

If it still fails, inspect `~/.m2/settings.xml` for a mirror, proxy, or offline policy and confirm that Maven can reach `repo.maven.apache.org`. Maven Central `1.1.0` is a different, legacy template.

## The generated project uses the wrong Java version

Maven can run under a different JDK than the shell's first `java`:

```bash
java -version
mvn -version
```

Both should report JDK 21. Set `JAVA_HOME` explicitly and restart the IDE/terminal if they differ.

## The application has no active profile

This is intentional; `dev` is no longer activated implicitly. Start with:

```bash
SPRING_PROFILES_ACTIVE=dev sh ./mvnw -f start/pom.xml spring-boot:run
```

For production, set `SPRING_PROFILES_ACTIVE=prod` and all three required datasource environment variables. Missing production credentials should stop startup rather than fall back to a local/root account.

## API requests return 401 Unauthorized

Business APIs are fail-closed. In local development, all of the following are required:

```bash
SPRING_PROFILES_ACTIVE=dev
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true
```

and on the request:

```text
X-Dev-User-Id: 1001
X-Dev-Tenant-Id: 42
```

Both IDs must be positive integers. The old `X-User-Id`, `X-Tenant-Id`, and `X-Admin` headers are deliberately ignored.

Under `prod`, trusted headers remain unavailable even if a client sends them. Configure a real Spring Security authentication mechanism and map its verified principal to `AuthenticatedCaller`.

## API requests return 403 Forbidden

The caller is authenticated but lacks the required authority:

| Operation | Authority |
|---|---|
| Read/list users | `users:read` |
| Create/update users | `users:write` |
| Delete users | `users:delete` |

The dev/test authorities come from `atom.security.trusted-header.authorities`, not from request headers. For production, verify the IdP claim-to-authority mapping.

## A user exists but cannot be found

Check the tenant used for both write and read. Repository and cache keys are tenant-scoped, so the same user ID under another `TenantId` is intentionally invisible. Do not fix this by removing the tenant predicate; fix caller-to-tenant mapping or use a separately authorized cross-tenant administration use case.

## MySQL connection fails

Start the generated Compose service and inspect its status:

```bash
docker compose up -d mysql
docker compose ps
docker compose logs mysql
```

The development profile expects MySQL on `localhost:3306` with the credentials in `conf/application-dev.yml`. If port 3306 is already used, either stop the conflicting service or change both the Compose mapping and JDBC URL.

## Flyway migration fails

Inspect application logs and the `flyway_schema_history` table. Common causes are:

- a manually created schema differs from the migration;
- an applied migration file was edited and its checksum changed;
- the database account cannot create/alter objects;
- a new `NOT NULL` or unique constraint conflicts with existing data.

Do not add a second `schema.sql` to work around Flyway. For an existing database, create a deliberate baseline and forward migration. Use Flyway repair only after confirming why the recorded checksum/state is wrong.

## Redis connection errors

Redis is disabled by default. If caching is unnecessary, ensure:

```bash
ATOM_REDIS_ENABLED=false
```

The no-op cache should allow the application to start and operate without Redis. If Redis is enabled, start it and verify the Spring Data Redis connection settings:

```bash
docker compose up -d redis
docker compose logs redis
```

Do not make authorization, uniqueness, or aggregate correctness depend on a cache hit or Redis lock.

## An update returns a conflict

An optimistic-lock conflict means another transaction changed the aggregate after it was read. Reload current state, re-evaluate the command, and retry only if repeating the use case is safe. Do not overwrite the version or silently treat a zero-row update as success.

## Integration tests are skipped

Container-backed integration tests are explicitly gated:

```bash
CI=true sh ./mvnw test
```

Docker must be running. Unit tests still run without `CI=true`.

## Swagger UI is unavailable

Use:

```text
http://localhost:8080/swagger-ui/index.html
```

The JSON document is at `http://localhost:8080/v3/api-docs`. Confirm the application is running and the `infra/rest` module is included by `start`. The production profile disables both endpoints by default; enable them explicitly only with an appropriate access policy.

## Maven reports dependency convergence or missing Boot 4 classes

Run:

```bash
sh ./mvnw dependency:tree
sh ./mvnw help:effective-pom
```

Avoid pinning individual Spring artifacts outside the Spring Boot BOM. When moving to Boot 4, update the parent, modular starters, MyBatis-Plus starter, SpringDoc, Jackson APIs, and test dependencies as one compatible set.

## Velocity placeholders are wrong in a generated file

This section is for archetype maintainers. Template variables such as `${package}` and `${artifactId}` are expected in `src/main/resources/archetype-resources`.

For a literal Spring placeholder in a Velocity-filtered YAML template, use:

```velocity
#set( $dollar = '$' )
password: ${dollar}{SPRING_DATASOURCE_PASSWORD}
```

After modifying a template:

```bash
make install
make demo
cd ~/Downloads/atom-demo
sh ./mvnw compile
CI=true sh ./mvnw test
```

If a new file does not appear, update `archetype-metadata.xml`. If a sample file was renamed or removed, update `clean.sh` as well.
