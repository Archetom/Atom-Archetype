package ${package}.domain.entity;

import ${package}.api.enums.UserStatus;
import ${package}.domain.event.UserCreatedEvent;
import ${package}.domain.event.UserStatusChangedEvent;
import ${package}.domain.exception.UserDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户实体单元测试
 * @author hanfeng
 */
@DisplayName("用户实体测试")
class UserTest {

    @Test
    @DisplayName("创建用户 - 成功")
    void createUser_Success() {
        // When
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // Then
        assertNotNull(user);
        assertEquals("testuser", user.getUsernameValue());
        assertEquals("test@example.com", user.getEmailValue());
        assertEquals("Test User", user.getRealName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());

        // 验证值对象
        assertNotNull(user.getUsername());
        assertNotNull(user.getEmail());
        assertEquals("testuser", user.getUsername().getValue());
        assertEquals("test@example.com", user.getEmail().getValue());

        // 验证领域事件
        assertEquals(1, user.getDomainEvents().size());
        assertInstanceOf(UserCreatedEvent.class, user.getDomainEvents().get(0));
    }

    @Test
    @DisplayName("创建用户 - 用户名为空")
    void createUser_EmptyUsername() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("", "test@example.com", "password123", "Test User");
        });
    }

    @Test
    @DisplayName("创建用户 - 邮箱为空")
    void createUser_EmptyEmail() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("testuser", "", "password123", "Test User");
        });
    }

    @Test
    @DisplayName("创建用户 - 密码为空")
    void createUser_EmptyPassword() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("testuser", "test@example.com", "", "Test User");
        });
    }

    @Test
    @DisplayName("创建用户 - 用户名太短")
    void createUser_UsernameTooShort() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("ab", "test@example.com", "password123", "Test User");
        });
    }

    @Test
    @DisplayName("创建用户 - 密码太短")
    void createUser_PasswordTooShort() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("testuser", "test@example.com", "123", "Test User");
        });
    }

    @Test
    @DisplayName("创建用户 - 带手机号")
    void createUser_WithPhoneNumber() {
        // When
        User user = User.create("testuser", "test@example.com", "13800138000", "password123", "Test User");

        // Then
        assertNotNull(user);
        assertEquals("testuser", user.getUsernameValue());
        assertEquals("test@example.com", user.getEmailValue());
        assertEquals("13800138000", user.getPhoneNumberValue());
        assertEquals("138****8000", user.getMaskedPhoneNumber());
        assertEquals("Test User", user.getRealName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    @DisplayName("更改用户状态 - 成功")
    void changeStatus_Success() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.clearDomainEvents(); // 清除创建事件

        // When
        user.changeStatus(UserStatus.LOCKED, "违规操作");

        // Then
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertTrue(user.isLocked());

        // 验证状态变更事件
        assertEquals(1, user.getDomainEvents().size());
        UserStatusChangedEvent event = (UserStatusChangedEvent) user.getDomainEvents().get(0);
        assertEquals(UserStatus.ACTIVE, event.getOldStatus());
        assertEquals(UserStatus.LOCKED, event.getNewStatus());
        assertEquals("违规操作", event.getReason());
    }

    @Test
    @DisplayName("激活用户")
    void activate_Success() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.changeStatus(UserStatus.INACTIVE, "测试");
        user.clearDomainEvents();

        // When
        user.activate();

        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("锁定用户")
    void lock_Success() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.clearDomainEvents();

        // When
        user.lock("违规操作");

        // Then
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertTrue(user.isLocked());
    }

    @Test
    @DisplayName("删除用户")
    void delete_Success() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.clearDomainEvents();

        // When
        user.delete();

        // Then
        assertEquals(UserStatus.DELETED, user.getStatus());
        assertTrue(user.isDeleted());
    }

    @Test
    @DisplayName("删除已删除的用户 - 失败")
    void deleteDeletedUser_Fail() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.delete();

        // When & Then
        assertThrows(UserDomainException.class, () -> {
            user.changeStatus(UserStatus.ACTIVE, "重新激活");
        });
    }

    @Test
    @DisplayName("更改邮箱 - 字符串版本")
    void changeEmail_String() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // When
        user.changeEmail("newemail@example.com");

        // Then
        assertEquals("newemail@example.com", user.getEmailValue());
        assertNotNull(user.getEmail());
        assertEquals("newemail@example.com", user.getEmail().getValue());
    }

    @Test
    @DisplayName("更改手机号 - 字符串版本")
    void changePhoneNumber_String() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // When
        user.changePhoneNumber("13900139000");

        // Then
        assertEquals("13900139000", user.getPhoneNumberValue());
        assertEquals("139****9000", user.getMaskedPhoneNumber());
        assertNotNull(user.getPhoneNumber());
        assertEquals("13900139000", user.getPhoneNumber().getValue());
    }

    @Test
    @DisplayName("管理员角色管理")
    void adminRole_Management() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // 初始状态不是管理员
        assertFalse(user.isAdmin());

        // When - 授予管理员角色
        user.grantAdminRole();

        // Then
        assertTrue(user.isAdmin());

        // When - 撤销管理员角色
        user.revokeAdminRole();

        // Then
        assertFalse(user.isAdmin());
    }

    @Test
    @DisplayName("外部用户管理")
    void externalUser_Management() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // 初始状态不是外部用户
        assertFalse(user.isExternalUser());

        // When - 标记为外部用户
        user.markAsExternalUser();
        user.setExternalId("EXT_001");

        // Then
        assertTrue(user.isExternalUser());
        assertEquals("EXT_001", user.getExternalId());
    }

    @Test
    @DisplayName("使用值对象创建用户")
    void createUserWithValidatedParams_Success() {
        // Given
        var username = new ${package}.domain.valueobject.Username("testuser");
        var email = new ${package}.domain.valueobject.Email("test@example.com");

        // When - 使用 createWithValidatedParams 方法
        User user = User.createWithValidatedParams(username, email, "encrypted_password_hash", "Test User");

        // Then
        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals("testuser", user.getUsernameValue());
        assertEquals("test@example.com", user.getEmailValue());
        assertEquals("Test User", user.getRealName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("encrypted_password_hash", user.getPassword()); // 这里是加密后的密码
    }

    @Test
    @DisplayName("使用值对象创建用户 - 带手机号")
    void createUserWithValidatedParams_WithPhone_Success() {
        // Given
        var username = new ${package}.domain.valueobject.Username("testuser");
        var email = new ${package}.domain.valueobject.Email("test@example.com");
        var phoneNumber = new ${package}.domain.valueobject.PhoneNumber("13800138000");

        // When
        User user = User.createWithValidatedParams(username, email, phoneNumber, "encrypted_password_hash", "Test User");

        // Then
        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals(phoneNumber, user.getPhoneNumber());
        assertEquals("testuser", user.getUsernameValue());
        assertEquals("test@example.com", user.getEmailValue());
        assertEquals("13800138000", user.getPhoneNumberValue());
        assertEquals("138****8000", user.getMaskedPhoneNumber());
        assertEquals("Test User", user.getRealName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }
}
