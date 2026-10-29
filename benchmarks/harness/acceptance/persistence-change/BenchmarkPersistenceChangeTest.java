package com.example.benchmark;

import com.example.benchmark.domain.entity.User;
import com.example.benchmark.domain.exception.AggregateVersionConflictException;
import com.example.benchmark.domain.exception.InvalidValueException;
import com.example.benchmark.domain.repository.UserRepository;
import com.example.benchmark.domain.valueobject.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestTokens.TrustedIssuer.class)
class BenchmarkPersistenceChangeTest extends BaseIntegrationTest {
    @Autowired UserRepository repository;

    @Test
    void migrationUpgradesExistingRowsWithoutRewritingHistory() {
        try (MySQLContainer upgrade = new MySQLContainer(DockerImageName.parse(mysqlContainer().getDockerImageName()))
                .withDatabaseName("upgrade_test").withUsername("test_user").withPassword("test_password")) {
            upgrade.start();
            Flyway.configure().dataSource(upgrade.getJdbcUrl(), upgrade.getUsername(), upgrade.getPassword())
                    .locations("classpath:benchmark-baseline-migrations").load().migrate();
            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                    upgrade.getJdbcUrl(), upgrade.getUsername(), upgrade.getPassword()));
            jdbc.update("INSERT INTO t_user (tenant_id, username, email, password_hash) VALUES (11, 'olduser', 'old@example.com', 'hash')");
            Flyway.configure().dataSource(upgrade.getJdbcUrl(), upgrade.getUsername(), upgrade.getPassword())
                    .locations("classpath:db/migration").load().migrate();
            assertEquals("en", jdbc.queryForObject("SELECT preferred_language FROM t_user WHERE username='olduser'", String.class));
            assertEquals(10L, jdbc.queryForObject("SELECT character_maximum_length FROM information_schema.columns "
                    + "WHERE table_schema=DATABASE() AND table_name='t_user' AND column_name='preferred_language'", Long.class));
            assertEquals("NO", jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns "
                    + "WHERE table_schema=DATABASE() AND table_name='t_user' AND column_name='preferred_language'", String.class));
            assertEquals("en", jdbc.queryForObject("SELECT column_default FROM information_schema.columns "
                    + "WHERE table_schema=DATABASE() AND table_name='t_user' AND column_name='preferred_language'", String.class));
            assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1", Integer.class) >= 2);
        }
    }

    @Test
    void defaultAndMutationRoundTripWithoutReconstructionEvents() {
        User user = fresh(11, "owner");
        assertEquals("en", language(user));
        changeLanguage(user, "fr");
        repository.save(user.getTenantId(), user);
        User restored = repository.findById(user.getTenantId(), user.getId()).orElseThrow();
        assertEquals("fr", language(restored));
        assertTrue(restored.getDomainEvents().isEmpty());
        assertEquals(user.getVersion(), restored.getVersion());
        changeLanguage(restored, "zh-CN");
        repository.save(restored.getTenantId(), restored);
        assertEquals("zh-CN", new JdbcTemplate(dataSource).queryForObject(
                "SELECT preferred_language FROM t_user WHERE id=?", String.class, user.getId().getValue()));
        assertEquals("zh-CN", language(repository.findById(user.getTenantId(), user.getId()).orElseThrow()));
    }

    @Test
    void mapperAndPublicQueriesExposeStoredLanguage() throws Exception {
        User user = fresh(11, "owner");
        changeLanguage(user, "de");
        repository.save(user.getTenantId(), user);
        User listed = repository.findUsers(user.getTenantId(), null, null, null, 1, 20).items().getFirst();
        assertEquals("de", language(listed));
        String token = "Bearer " + TestTokens.token(7, 11, "users:read");
        mockMvc.perform(get("/api/v1/users/{id}", user.getId().getValue()).header(AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.preferredLanguage").value("de"));
        mockMvc.perform(get("/api/v1/users").header(AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.objectList[0].preferredLanguage").value("de"));
    }

    @Test
    void tenantScopeCannotLeakOrOverwriteLanguage() throws Exception {
        User user = fresh(11, "owner");
        changeLanguage(user, "fr");
        repository.save(user.getTenantId(), user);
        TenantId other = new TenantId(22L);
        assertTrue(repository.findById(other, user.getId()).isEmpty());
        assertTrue(repository.findUsers(other, null, null, null, 1, 20).items().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> repository.save(other, user));
        mockMvc.perform(get("/api/v1/users/{id}", user.getId().getValue())
                        .header(AUTHORIZATION, "Bearer " + TestTokens.token(7, 22, "users:read")))
                .andExpect(status().isNotFound());
        assertEquals("fr", language(repository.findById(user.getTenantId(), user.getId()).orElseThrow()));
    }

    @Test
    void optimisticLockRejectsStaleLanguageUpdate() {
        User created = fresh(11, "owner");
        repository.save(created.getTenantId(), created);
        User first = repository.findById(created.getTenantId(), created.getId()).orElseThrow();
        User stale = repository.findById(created.getTenantId(), created.getId()).orElseThrow();
        changeLanguage(first, "fr");
        repository.save(first.getTenantId(), first);
        changeLanguage(stale, "de");
        assertThrows(AggregateVersionConflictException.class, () -> repository.save(stale.getTenantId(), stale));
        User restored = repository.findById(created.getTenantId(), created.getId()).orElseThrow();
        assertEquals("fr", language(restored));
        assertEquals(1L, restored.getVersion());
    }

    @Test
    void invalidLanguageCannotChangeTheAggregate() {
        User user = fresh(11, "owner");
        Method change = changeMethod();
        for (String invalid : new String[]{null, "", " ", "elevenchars!"}) {
            InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                    () -> change.invoke(user, invalid));
            assertInstanceOf(InvalidValueException.class, failure.getCause());
            assertEquals("en", language(user));
        }
    }

    private User fresh(long tenant, String username) {
        return User.createWithPasswordHash(new TenantId(tenant), new Username(username),
                new Email(username + "@example.com"), PasswordHash.fromTrustedHash("trusted-benchmark-hash"), "Owner");
    }

    private String language(User user) {
        Method getter = assertDoesNotThrow(() -> User.class.getMethod("getPreferredLanguage"));
        return (String) assertDoesNotThrow(() -> getter.invoke(user));
    }

    private Method changeMethod() {
        return assertDoesNotThrow(() -> User.class.getMethod("changePreferredLanguage", String.class));
    }

    private void changeLanguage(User user, String value) {
        assertDoesNotThrow(() -> changeMethod().invoke(user, value));
    }

    @Override protected void initTestData() { truncateTable("t_user"); }
    @Override protected void clearTestData() { truncateTable("t_user"); }
}
