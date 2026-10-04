package ${package}.domain.factory;

import ${package}.domain.entity.User;
import ${package}.domain.exception.UserDomainException;
import ${package}.domain.policy.PasswordPolicy;
import ${package}.domain.service.UserDomainService;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PasswordHash;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.Username;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserFactoryTest {

    private static final TenantId TENANT_ID = new TenantId(1L);

    private final RecordingDomainService domainService = new RecordingDomainService();
    private final UserFactory factory = new UserFactory(domainService, new PasswordPolicy());

    @Test
    void rejectsAnInvalidPasswordBeforeQueryingUniqueness() {
        assertThrows(UserDomainException.class, () -> factory.createStandardUser(
                TENANT_ID, "alice", "alice@example.com", "too-short", "Alice"));

        assertFalse(domainService.uniquenessChecked);
    }

    @Test
    void createsAUserWithTheHashedPassword() {
        User user = factory.createStandardUser(
                TENANT_ID, "alice", "alice@example.com", "long-enough-password", "Alice");

        assertTrue(domainService.uniquenessChecked);
        assertEquals("hashed:long-enough-password", user.getPasswordHash().valueForPersistence());
    }

    /** Records repository-backed checks; every username and email is available. */
    private static final class RecordingDomainService implements UserDomainService {

        private boolean uniquenessChecked;

        @Override
        public boolean isUsernameAvailable(TenantId tenantId, Username username) {
            return true;
        }

        @Override
        public boolean isEmailAvailable(TenantId tenantId, Email email) {
            return true;
        }

        @Override
        public void validateUserCreation(TenantId tenantId, Username username, Email email) {
            uniquenessChecked = true;
        }

        @Override
        public PasswordHash hashPassword(String plainPassword) {
            return PasswordHash.fromTrustedHash("hashed:" + plainPassword);
        }

        @Override
        public boolean canDeleteUser(User user) {
            return true;
        }
    }
}
