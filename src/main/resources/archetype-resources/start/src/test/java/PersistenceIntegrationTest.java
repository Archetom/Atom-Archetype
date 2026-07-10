package ${package};

import ${package}.domain.entity.User;
import ${package}.domain.exception.AggregateVersionConflictException;
import ${package}.domain.exception.UserAlreadyExistsException;
import ${package}.domain.model.UserStatus;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PasswordHash;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class PersistenceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void flywayCreatesTheSingleAuthoritativeSchema() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer migrations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        Integer versionColumns = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 't_user' AND column_name = 'version'",
                Integer.class);
        Integer deletedColumns = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 't_user' AND column_name = 'deleted_time'",
                Integer.class);

        assertTrue(migrations != null && migrations >= 1);
        assertEquals(1, versionColumns);
        assertEquals(0, deletedColumns);
    }

    @Test
    void roundTripsFieldsAndIsolatesTenants() {
        TenantId tenantA = new TenantId(1L);
        TenantId tenantB = new TenantId(2L);
        User userA = newUser(tenantA, "alice", "alice@example.com");

        userRepository.save(tenantA, userA);

        assertNotNull(userA.getId());
        assertEquals(0L, userA.getVersion());
        assertTrue(userRepository.findById(tenantA, userA.getId()).isPresent());
        assertTrue(userRepository.findById(tenantB, userA.getId()).isEmpty());

        User userB = newUser(tenantB, "alice", "alice@example.com");
        assertDoesNotThrow(() -> userRepository.save(tenantB, userB));
    }

    @Test
    void translatesDatabaseUniqueConstraintsToDomainConflicts() {
        TenantId tenantId = new TenantId(1L);
        userRepository.save(tenantId, newUser(tenantId, "alice", "alice@example.com"));

        UserAlreadyExistsException usernameConflict = assertThrows(
                UserAlreadyExistsException.class,
                () -> userRepository.save(
                        tenantId, newUser(tenantId, "alice", "different@example.com")));
        assertEquals("Username already exists", usernameConflict.getMessage());

        UserAlreadyExistsException emailConflict = assertThrows(
                UserAlreadyExistsException.class,
                () -> userRepository.save(
                        tenantId, newUser(tenantId, "different", "alice@example.com")));
        assertEquals("Email already exists", emailConflict.getMessage());
    }

    @Test
    void detectsOptimisticLockConflict() {
        TenantId tenantId = new TenantId(1L);
        User created = userRepository.save(
                tenantId, newUser(tenantId, "alice", "alice@example.com"));
        User first = userRepository.findById(tenantId, created.getId()).orElseThrow();
        User stale = userRepository.findById(tenantId, created.getId()).orElseThrow();

        first.lock("security review");
        userRepository.save(tenantId, first);
        assertEquals(1L, first.getVersion());

        stale.changeStatus(UserStatus.INACTIVE, "stale update");
        assertThrows(AggregateVersionConflictException.class,
                () -> userRepository.save(tenantId, stale));
    }

    @Test
    void softDeleteUsesDomainStatusOnly() {
        TenantId tenantId = new TenantId(1L);
        User user = userRepository.save(
                tenantId, newUser(tenantId, "alice", "alice@example.com"));

        user.delete();
        userRepository.save(tenantId, user);

        User deleted = userRepository.findById(tenantId, new UserId(user.getId().getValue()))
                .orElseThrow();
        assertTrue(deleted.isDeleted());
        assertTrue(userRepository.findUsers(tenantId, null, null, null, 1, 20)
                .items().isEmpty());
    }

    private User newUser(TenantId tenantId, String username, String email) {
        return User.createWithPasswordHash(
                tenantId,
                new Username(username),
                new Email(email),
                PasswordHash.fromTrustedHash("encrypted-password-hash"),
                "Test User");
    }

    @Override
    protected void initTestData() {
        truncateTable("t_user");
    }

    @Override
    protected void clearTestData() {
        truncateTable("t_user");
    }
}
