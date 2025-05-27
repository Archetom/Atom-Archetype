package ${package}.domain.specification;

import ${package}.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户规约测试
 * @author hanfeng
 */
@DisplayName("用户规约测试")
class UserSpecificationTest {

    @Test
    @DisplayName("用户可以登录 - 激活用户")
    void canLogin_ActiveUser() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // When
        boolean canLogin = UserSpecification.canLogin().isSatisfiedBy(user);

        // Then
        assertTrue(canLogin);
    }

    @Test
    @DisplayName("用户不能登录 - 锁定用户")
    void cannotLogin_LockedUser() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.lock("违规操作");

        // When
        boolean canLogin = UserSpecification.canLogin().isSatisfiedBy(user);

        // Then
        assertFalse(canLogin);
    }

    @Test
    @DisplayName("组合规约 - 可登录且属于指定租户")
    void compositeSpecification_CanLoginAndBelongsToTenant() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.setTenantId(1L);

        Specification<User> spec = UserSpecification.canLogin()
                .and(UserSpecification.belongsToTenant(1L));

        // When
        boolean satisfied = spec.isSatisfiedBy(user);

        // Then
        assertTrue(satisfied);
    }
}
