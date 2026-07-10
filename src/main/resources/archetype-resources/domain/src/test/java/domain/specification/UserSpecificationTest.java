package ${package}.domain.specification;

import ${package}.domain.entity.User;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PasswordHash;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests composable User specifications without infrastructure dependencies. */
@DisplayName("User specifications")
class UserSpecificationTest {

    private static final TenantId TENANT_ID = new TenantId(1L);

    @Test
    @DisplayName("active user can log in")
    void canLogin_ActiveUser() {
        // Given
        User user = persistedUser();

        // When
        boolean canLogin = UserSpecification.canLogin().isSatisfiedBy(user);

        // Then
        assertTrue(canLogin);
    }

    @Test
    @DisplayName("locked user cannot log in")
    void cannotLogin_LockedUser() {
        // Given
        User user = persistedUser();
        user.lock("");

        // When
        boolean canLogin = UserSpecification.canLogin().isSatisfiedBy(user);

        // Then
        assertFalse(canLogin);
    }

    @Test
    @DisplayName("specifications compose with tenant membership")
    void compositeSpecification_CanLoginAndBelongsToTenant() {
        // Given
        User user = persistedUser();
        Specification<User> spec = UserSpecification.canLogin()
                .and(UserSpecification.belongsToTenant(TENANT_ID));

        // When
        boolean satisfied = spec.isSatisfiedBy(user);

        // Then
        assertTrue(satisfied);
    }

    private User persistedUser() {
        User user = User.createWithPasswordHash(
                TENANT_ID,
                new Username("testuser"),
                new Email("test@example.com"),
                PasswordHash.fromTrustedHash("test-password-hash"),
                "Test User");
        user.onPersisted(new UserId(100L), 0L, user.getCreatedTime(), user.getUpdatedTime());
        user.clearDomainEvents();
        return user;
    }
}
