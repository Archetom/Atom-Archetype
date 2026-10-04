package ${package}.domain.factory;

import ${package}.domain.entity.User;
import ${package}.domain.policy.PasswordPolicy;
import ${package}.domain.service.UserDomainService;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PasswordHash;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.Username;

/**
 * Creates valid user aggregates that require domain services or password policy checks.
 */
public class UserFactory {

    private final UserDomainService userDomainService;
    private final PasswordPolicy passwordPolicy;

    public UserFactory(UserDomainService userDomainService,
                       PasswordPolicy passwordPolicy) {
        this.userDomainService = userDomainService;
        this.passwordPolicy = passwordPolicy;
    }

    /**
     * Creates a standard user after validating the password policy and identity uniqueness.
     * Input is validated first, so a rejected request never queries the repository.
     */
    public User createStandardUser(TenantId tenantId, String username, String email, String password, String realName) {
        Username usernameValue = new Username(username);
        Email emailValue = new Email(email);
        passwordPolicy.validate(password);

        userDomainService.validateUserCreation(tenantId, usernameValue, emailValue);

        PasswordHash passwordHash = userDomainService.hashPassword(password);
        return User.createWithPasswordHash(
                tenantId, usernameValue, emailValue, passwordHash, realName);
    }

    /** Creates a standard user with a validated phone number. */
    public User createUserWithPhone(TenantId tenantId, String username, String email, String phoneNumber,
                                    String password, String realName) {
        Username usernameValue = new Username(username);
        Email emailValue = new Email(email);
        PhoneNumber phoneNumberValue = new PhoneNumber(phoneNumber);
        passwordPolicy.validate(password);

        userDomainService.validateUserCreation(tenantId, usernameValue, emailValue);

        PasswordHash passwordHash = userDomainService.hashPassword(password);
        return User.createWithPasswordHash(
                tenantId, usernameValue, emailValue, phoneNumberValue, passwordHash, realName);
    }
}
