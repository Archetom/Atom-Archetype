package ${package}.domain.specification;

import ${package}.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * user specification test
 * @author hanfeng
 */
@DisplayName(" user specification test ")
class UserSpecificationTest {

    @Test
    @DisplayName(" user can - active user ")
    void canLogin_ActiveUser() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // When
        boolean canLogin = UserSpecification.canLogin().isSatisfiedBy(user);

        // Then
        assertTrue(canLogin);
    }

    @Test
    @DisplayName(" user cannot - locked user ")
    void cannotLogin_LockedUser() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.lock("");

        // When
        boolean canLogin = UserSpecification.canLogin().isSatisfiedBy(user);

        // Then
        assertFalse(canLogin);
    }

    @Test
    @DisplayName(" specification - can and in tenant ")
    void compositeSpecification_CanLoginAndBelongsToTenant() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.changeTenantId(1L);

        Specification<User> spec = UserSpecification.canLogin()
                .and(UserSpecification.belongsToTenant(1L));

        // When
        boolean satisfied = spec.isSatisfiedBy(user);

        // Then
        assertTrue(satisfied);
    }
}
