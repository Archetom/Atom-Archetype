package ${package}.domain.entity;

import ${package}.api.enums.UserStatus;
import ${package}.domain.aggregate.AggregateRoot;
import ${package}.domain.event.UserCreatedEvent;
import ${package}.domain.event.UserStatusChangedEvent;
import ${package}.domain.exception.UserDomainException;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import lombok.Getter;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/**
 * user aggregate root
 * @author hanfeng
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class User extends AggregateRoot<UserId> {

    private UserId id;
    private Username username;
    private Email email;
    private PhoneNumber phoneNumber;
    private String password;
    private String realName;
    private UserStatus status;
    private Long tenantId;
    private String externalId; // external system ID
    private boolean externalUser; // whether External User
    private boolean admin; // whether administrator
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // without function - framework (MapStruct, JPA etc.)
    public User() {
        // framework function, business validate
    }

    // ========== override Lombok generate of getter method class ==========

    /**
     * get username value object
     */
    public Username getUsername() {
        return this.username;
    }

    /**
     * get email value object
     */
    public Email getEmail() {
        return this.email;
    }

    /**
     * get phone number value object
     */
    public PhoneNumber getPhoneNumber() {
        return this.phoneNumber;
    }

    /**
     * get user ID value object
     */
    @Override
    public UserId getId() {
        return this.id;
    }

    // ========== convenience method get string value ==========

    /**
     * get username string value
     */
    public String getUsernameValue() {
        return username != null ? username.getValue() : null;
    }

    /**
     * get email string value
     */
    public String getEmailValue() {
        return email != null ? email.getValue() : null;
    }

    /**
     * get phone number string value
     */
    public String getPhoneNumberValue() {
        return phoneNumber != null ? phoneNumber.getValue() : null;
    }

    /**
     * get masked phone number
     */
    public String getMaskedPhoneNumber() {
        return phoneNumber != null ? phoneNumber.getMasked() : null;
    }

    // ========== method (simple) ==========

    /**
     * create user (simple method, used for test or simple)
     */
    public static User create(String username, String email, String password, String realName) {
        validateCreateParams(username, email, password);

        User user = new User();
        user.username = new Username(username);
        user.email = new Email(email);
        user.password = password;
        user.realName = realName;
        user.status = UserStatus.ACTIVE;
        user.externalUser = false;
        user.admin = false;
        user.createdTime = LocalDateTime.now();
        user.updatedTime = LocalDateTime.now();

        // add domain event
        user.addDomainEvent(new UserCreatedEvent(user.getId() != null ? user.getId().getValue() : null, username, email));

        return user;
    }

    /**
     * create user (with phone number, simple)
     */
    public static User create(String username, String email, String phoneNumber, String password, String realName) {
        User user = create(username, email, password, realName);
        if (StringUtils.isNotBlank(phoneNumber)) {
            user.phoneNumber = new PhoneNumber(phoneNumber);
        }
        return user;
    }

    /**
     * internal method - used for already of parameter (UserFactory)
     */
    public static User createWithValidatedParams(Username username, Email email, String encryptedPassword, String realName) {
        User user = new User();
        user.username = username;
        user.email = email;
        user.password = encryptedPassword;
        user.realName = realName;
        user.status = UserStatus.ACTIVE;
        user.externalUser = false;
        user.admin = false;
        user.createdTime = LocalDateTime.now();
        user.updatedTime = LocalDateTime.now();

        // add domain event
        user.addDomainEvent(new UserCreatedEvent(
                user.getId() != null ? user.getId().getValue() : null,
                username.getValue(),
                email.getValue()
        ));

        return user;
    }

    /**
     * internal method - with phone number
     */
    public static User createWithValidatedParams(Username username, Email email, PhoneNumber phoneNumber,
                                          String encryptedPassword, String realName) {
        User user = createWithValidatedParams(username, email, encryptedPassword, realName);
        user.phoneNumber = phoneNumber;
        return user;
    }

    /**
     * reconstitute method - from layer reconstitute domain object (domain event)
     */
    public static User reconstitute(UserId id, Username username, Email email, PhoneNumber phoneNumber,
                                     String password, String realName, UserStatus status,
                                     Long tenantId, String externalId, boolean externalUser,
                                     boolean admin, LocalDateTime createdTime, LocalDateTime updatedTime) {
        User user = new User();
        user.id = id;
        user.username = username;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.password = password;
        user.realName = realName;
        user.status = status;
        user.tenantId = tenantId;
        user.externalId = externalId;
        user.externalUser = externalUser;
        user.admin = admin;
        user.createdTime = createdTime;
        user.updatedTime = updatedTime;
        return user;
    }

    // ========== business method ==========

    /**
     * update status
     */
    public void changeStatus(UserStatus newStatus, String reason) {
        if (newStatus == null) {
            throw new UserDomainException("User status must not be empty");
        }

        if (this.status == UserStatus.DELETED) {
            throw new UserDomainException("Deleted users cannot change status");
        }

        if (this.status == newStatus) {
            return; // status not, no need process
        }

        UserStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedTime = LocalDateTime.now();

        // add status event
        addDomainEvent(new UserStatusChangedEvent(this.getId() != null ? this.getId().getValue() : null, oldStatus, newStatus, reason));
    }

    /**
     * active user
     */
    public void activate() {
        changeStatus(UserStatus.ACTIVE, " user active ");
    }

    /**
     * locked user
     */
    public void lock(String reason) {
        changeStatus(UserStatus.LOCKED, reason);
    }

    /**
     * delete user (soft delete)
     */
    public void delete() {
        changeStatus(UserStatus.DELETED, " user delete ");
    }

    /**
     * email (value object)
     */
    public void changeEmail(Email newEmail) {
        if (newEmail == null) {
            throw new UserDomainException("Email must not be empty");
        }

        if (!newEmail.sameValueAs(this.email)) {
            this.email = newEmail;
            this.updatedTime = LocalDateTime.now();
        }
    }

    /**
     * email (string)
     */
    public void changeEmail(String newEmail) {
        Email newEmailVO = new Email(newEmail);
        if (!newEmailVO.sameValueAs(this.email)) {
            this.email = newEmailVO;
            this.updatedTime = LocalDateTime.now();
        }
    }

    /**
     * phone number (value object)
     */
    public void changePhoneNumber(PhoneNumber newPhoneNumber) {
        if (newPhoneNumber != null && !newPhoneNumber.sameValueAs(this.phoneNumber)) {
            this.phoneNumber = newPhoneNumber;
            this.updatedTime = LocalDateTime.now();
        }
    }

    /**
     * update phone number (string)
     */
    public void changePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = new PhoneNumber(newPhoneNumber);
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * set external system ID
     */
    public void changeExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * set tenant ID
     */
    public void changeTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * as External User
     */
    public void markAsExternalUser() {
        this.externalUser = true;
    }

    /**
     * administrator role
     */
    public void grantAdminRole() {
        this.admin = true;
    }

    /**
     * administrator role
     */
    public void revokeAdminRole() {
        this.admin = false;
    }

    // ========== status check method ==========

    /**
     * check user whether active
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    /**
     * check user whether locked
     */
    public boolean isLocked() {
        return UserStatus.LOCKED.equals(this.status);
    }

    /**
     * check user whether delete
     */
    public boolean isDeleted() {
        return UserStatus.DELETED.equals(this.status);
    }

    /**
     * check whether as administrator
     */
    public boolean isAdmin() {
        return this.admin;
    }

    /**
     * check whether as External User
     */
    public boolean isExternalUser() {
        return this.externalUser;
    }

    // ========== validate method (used for simple method) ==========

    /**
     * validate create parameter (used for simple method)
     */
    private static void validateCreateParams(String username, String email, String password) {
        if (StringUtils.isBlank(username)) {
            throw new UserDomainException("Username must not be empty");
        }
        if (StringUtils.isBlank(email)) {
            throw new UserDomainException("Email must not be empty");
        }
        if (StringUtils.isBlank(password)) {
            throw new UserDomainException("Password must not be empty");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new UserDomainException("Username length must be between3-50 characters ");
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new UserDomainException("Password length must be between6-20 characters ");
        }
    }
}
