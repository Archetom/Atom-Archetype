package ${package}.domain.entity;

import ${package}.api.enums.UserStatus;
import ${package}.domain.event.UserCreatedEvent;
import ${package}.domain.event.UserStatusChangedEvent;
import ${package}.domain.exception.UserDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * user entity unit test
 * @author hanfeng
 */
@DisplayName(" user entity test ")
class UserTest {

    @Test
    @DisplayName(" create user - success ")
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

        // validate value object
        assertNotNull(user.getUsername());
        assertNotNull(user.getEmail());
        assertEquals("testuser", user.getUsername().getValue());
        assertEquals("test@example.com", user.getEmail().getValue());

        // validate domain event
        assertEquals(1, user.getDomainEvents().size());
        assertInstanceOf(UserCreatedEvent.class, user.getDomainEvents().get(0));
    }

    @Test
    @DisplayName(" create user - username is empty ")
    void createUser_EmptyUsername() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("", "test@example.com", "password123", "Test User");
        });
    }

    @Test
    @DisplayName(" create user - email is empty ")
    void createUser_EmptyEmail() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("testuser", "", "password123", "Test User");
        });
    }

    @Test
    @DisplayName(" create user - password is empty ")
    void createUser_EmptyPassword() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("testuser", "test@example.com", "", "Test User");
        });
    }

    @Test
    @DisplayName(" create user - username ")
    void createUser_UsernameTooShort() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("ab", "test@example.com", "password123", "Test User");
        });
    }

    @Test
    @DisplayName(" create user - password ")
    void createUser_PasswordTooShort() {
        // When & Then
        assertThrows(UserDomainException.class, () -> {
            User.create("testuser", "test@example.com", "123", "Test User");
        });
    }

    @Test
    @DisplayName(" create user - with phone number ")
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
    @DisplayName(" user status - success ")
    void changeStatus_Success() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.clearDomainEvents(); // clear create event

        // When
        user.changeStatus(UserStatus.LOCKED, "");

        // Then
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertTrue(user.isLocked());

        // validate status event
        assertEquals(1, user.getDomainEvents().size());
        UserStatusChangedEvent event = (UserStatusChangedEvent) user.getDomainEvents().get(0);
        assertEquals(UserStatus.ACTIVE, event.getOldStatus());
        assertEquals(UserStatus.LOCKED, event.getNewStatus());
        assertEquals("", event.getReason());
    }

    @Test
    @DisplayName(" active user ")
    void activate_Success() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.changeStatus(UserStatus.INACTIVE, " test ");
        user.clearDomainEvents();

        // When
        user.activate();

        // Then
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName(" locked user ")
    void lock_Success() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.clearDomainEvents();

        // When
        user.lock("");

        // Then
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertTrue(user.isLocked());
    }

    @Test
    @DisplayName(" delete user ")
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
    @DisplayName(" delete deleted of user - failure ")
    void deleteDeletedUser_Fail() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");
        user.delete();

        // When & Then
        assertThrows(UserDomainException.class, () -> {
            user.changeStatus(UserStatus.ACTIVE, " new active ");
        });
    }

    @Test
    @DisplayName(" email - string ")
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
    @DisplayName(" phone number - string ")
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
    @DisplayName(" administrator role ")
    void adminRole_Management() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // status administrator
        assertFalse(user.isAdmin());

        // When - administrator role
        user.grantAdminRole();

        // Then
        assertTrue(user.isAdmin());

        // When - administrator role
        user.revokeAdminRole();

        // Then
        assertFalse(user.isAdmin());
    }

    @Test
    @DisplayName("External User")
    void externalUser_Management() {
        // Given
        User user = User.create("testuser", "test@example.com", "password123", "Test User");

        // status External User
        assertFalse(user.isExternalUser());

        // When - as External User
        user.markAsExternalUser();
        user.changeExternalId("EXT_001");

        // Then
        assertTrue(user.isExternalUser());
        assertEquals("EXT_001", user.getExternalId());
    }

    @Test
    @DisplayName(" value object create user ")
    void createUserWithValidatedParams_Success() {
        // Given
        var username = new ${package}.domain.valueobject.Username("testuser");
        var email = new ${package}.domain.valueobject.Email("test@example.com");

        // When - createWithValidatedParams method
        User user = User.createWithValidatedParams(username, email, "encrypted_password_hash", "Test User");

        // Then
        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals("testuser", user.getUsernameValue());
        assertEquals("test@example.com", user.getEmailValue());
        assertEquals("Test User", user.getRealName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("encrypted_password_hash", user.getPassword()); // encrypted of password
    }

    @Test
    @DisplayName(" value object create user - with phone number ")
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
