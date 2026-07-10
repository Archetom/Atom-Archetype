package ${package}.domain.entity;

import ${package}.domain.model.UserStatus;
import ${package}.domain.aggregate.AggregateRoot;
import ${package}.domain.event.UserCreatedEvent;
import ${package}.domain.event.UserStatusChangedEvent;
import ${package}.domain.exception.UserDomainException;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PasswordHash;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import lombok.Getter;

import java.time.LocalDateTime;

/** Tenant-scoped User aggregate with explicit lifecycle behavior. */
@Getter
public class User extends AggregateRoot<UserId> {

    private UserId id;
    private Username username;
    private Email email;
    private PhoneNumber phoneNumber;
    private PasswordHash passwordHash;
    private String realName;
    private UserStatus status;
    private TenantId tenantId;
    /** Optional external-system reference; it is not a unique identity key. */
    private String externalId;
    private boolean externalUser;
    private boolean admin;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /** Constructor reserved for mapping and persistence frameworks. */
    public User() {
        // Business code creates users through a validated factory.
    }

    // ========== Value-object accessors ==========

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

    /**
     * Creates an aggregate from validated identity values and a trusted one-way hash.
     */
    public static User createWithPasswordHash(TenantId tenantId, Username username, Email email,
                                              PasswordHash passwordHash, String realName) {
        User user = new User();
        user.tenantId = requireTenantId(tenantId);
        user.username = requireUsername(username);
        user.email = requireEmail(email);
        user.passwordHash = requirePasswordHash(passwordHash);
        user.realName = realName;
        user.status = UserStatus.ACTIVE;
        user.externalUser = false;
        user.admin = false;
        user.createdTime = LocalDateTime.now();
        user.updatedTime = LocalDateTime.now();

        return user;
    }

    /**
     * Creates an aggregate with a validated E.164 phone number.
     */
    public static User createWithPasswordHash(TenantId tenantId, Username username, Email email,
                                              PhoneNumber phoneNumber, PasswordHash passwordHash,
                                              String realName) {
        User user = createWithPasswordHash(tenantId, username, email, passwordHash, realName);
        user.phoneNumber = phoneNumber;
        return user;
    }

    /** Restore persisted state without raising new domain events. */
    public static User reconstitute(UserId id, Username username, Email email, PhoneNumber phoneNumber,
                                     PasswordHash passwordHash, String realName, UserStatus status,
                                     TenantId tenantId, String externalId, boolean externalUser,
                                     boolean admin, Long version,
                                     LocalDateTime createdTime, LocalDateTime updatedTime) {
        User user = new User();
        user.id = id;
        user.username = username;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.passwordHash = requirePasswordHash(passwordHash);
        user.realName = realName;
        user.status = status;
        user.tenantId = requireTenantId(tenantId);
        user.externalId = externalId;
        user.externalUser = externalUser;
        user.admin = admin;
        user.restoreVersion(version);
        user.createdTime = createdTime;
        user.updatedTime = updatedTime;
        return user;
    }

    // ========== Business behavior ==========

    /** Change to a non-deleted status. */
    public void changeStatus(UserStatus newStatus, String reason) {
        if (newStatus == UserStatus.DELETED) {
            throw new UserDomainException("Use the delete operation to delete a user");
        }
        transitionTo(newStatus, reason);
    }

    private void transitionTo(UserStatus newStatus, String reason) {
        if (newStatus == null) {
            throw new UserDomainException("User status must not be empty");
        }

        if (this.status == UserStatus.DELETED) {
            throw new UserDomainException("Deleted users cannot change status");
        }

        if (this.status == newStatus) {
            return;
        }

        if (this.id == null) {
            throw new UserDomainException("User must be persisted before changing status");
        }

        UserStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedTime = LocalDateTime.now();

        addDomainEvent(new UserStatusChangedEvent(
                this.id.getValue(), this.tenantId.getValue(), oldStatus, newStatus, reason));
    }

    /**
     * Applies database-generated state to this aggregate after a successful
     * insert or update. A creation event is raised only after the ID exists.
     */
    public void onPersisted(UserId persistedId, Long persistedVersion,
                            LocalDateTime persistedCreatedTime,
                            LocalDateTime persistedUpdatedTime) {
        if (persistedId == null) {
            throw new UserDomainException("Persisted user ID must not be empty");
        }

        boolean newlyPersisted = this.id == null;
        if (!newlyPersisted && !this.id.sameValueAs(persistedId)) {
            throw new UserDomainException("Persisted user ID cannot change");
        }

        this.id = persistedId;
        restoreVersion(persistedVersion);
        if (persistedCreatedTime != null) {
            this.createdTime = persistedCreatedTime;
        }
        if (persistedUpdatedTime != null) {
            this.updatedTime = persistedUpdatedTime;
        }

        if (newlyPersisted) {
            addDomainEvent(new UserCreatedEvent(
                    this.id.getValue(),
                    this.tenantId.getValue(),
                    this.username.getValue(),
                    this.email.getValue()));
        }
    }

    /** Activate the user. */
    public void activate() {
        changeStatus(UserStatus.ACTIVE, "user activated");
    }

    /** Lock the user for the supplied business reason. */
    public void lock(String reason) {
        changeStatus(UserStatus.LOCKED, reason);
    }

    /** Soft-delete the user through the deletion-specific transition. */
    public void delete() {
        transitionTo(UserStatus.DELETED, "user deleted");
    }

    /** Change the email using an already validated value object. */
    public void changeEmail(Email newEmail) {
        if (newEmail == null) {
            throw new UserDomainException("Email must not be empty");
        }

        if (!newEmail.sameValueAs(this.email)) {
            this.email = newEmail;
            this.updatedTime = LocalDateTime.now();
        }
    }

    /** Validate and change the email supplied as text. */
    public void changeEmail(String newEmail) {
        Email newEmailVO = new Email(newEmail);
        if (!newEmailVO.sameValueAs(this.email)) {
            this.email = newEmailVO;
            this.updatedTime = LocalDateTime.now();
        }
    }

    /** Change the optional phone number using a validated value object. */
    public void changePhoneNumber(PhoneNumber newPhoneNumber) {
        if (newPhoneNumber != null && !newPhoneNumber.sameValueAs(this.phoneNumber)) {
            this.phoneNumber = newPhoneNumber;
            this.updatedTime = LocalDateTime.now();
        }
    }

    /** Validate and change the phone number supplied as E.164 text. */
    public void changePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = new PhoneNumber(newPhoneNumber);
        this.updatedTime = LocalDateTime.now();
    }

    // ========== State queries ==========

    /** Return whether the user is active. */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    /** Return whether the user is locked. */
    public boolean isLocked() {
        return UserStatus.LOCKED.equals(this.status);
    }

    /** Return whether the user is soft-deleted. */
    public boolean isDeleted() {
        return UserStatus.DELETED.equals(this.status);
    }

    /** Return whether deletion rules classify this user as an administrator. */
    public boolean isAdmin() {
        return this.admin;
    }

    /** Return whether this record originated from an external system. */
    public boolean isExternalUser() {
        return this.externalUser;
    }

    private static TenantId requireTenantId(TenantId tenantId) {
        if (tenantId == null) {
            throw new UserDomainException("Tenant ID must not be empty");
        }
        return tenantId;
    }

    private static Username requireUsername(Username username) {
        if (username == null) {
            throw new UserDomainException("Username must not be empty");
        }
        return username;
    }

    private static Email requireEmail(Email email) {
        if (email == null) {
            throw new UserDomainException("Email must not be empty");
        }
        return email;
    }

    private static PasswordHash requirePasswordHash(PasswordHash passwordHash) {
        if (passwordHash == null) {
            throw new UserDomainException("Password hash must not be empty");
        }
        return passwordHash;
    }
}
