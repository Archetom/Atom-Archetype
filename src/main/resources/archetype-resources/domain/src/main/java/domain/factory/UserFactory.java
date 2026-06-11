package ${package}.domain.factory;

import ${package}.domain.entity.User;
import ${package}.domain.policy.PasswordPolicy;
import ${package}.domain.policy.UserCreationPolicy;
import ${package}.domain.service.UserDomainService;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * user factory - responsible for complex user object of create
 * @author hanfeng
 */
@Component
@RequiredArgsConstructor
public class UserFactory {

    private final UserDomainService userDomainService;
    private final PasswordPolicy passwordPolicy;
    private final UserCreationPolicy userCreationPolicy;

    /**
     * create standard user
     */
    public User createStandardUser(String username, String email, String password, String realName) {
        // create value object
        Username usernameVO = new Username(username);
        Email emailVO = new Email(email);

        // validate create policy
        userCreationPolicy.validateCreation(usernameVO, emailVO);

        // validate business rule
        userDomainService.validateUserCreation(username, email);

        // validate password policy
        passwordPolicy.validate(password);

        // password
        String encryptedPassword = userDomainService.encryptPassword(password);

        // internal method create user (skip password)
        User user = User.createWithValidatedParams(usernameVO, emailVO, encryptedPassword, realName);

        return user;
    }

    /**
     * create with phone number of user
     */
    public User createUserWithPhone(String username, String email, String phoneNumber,
                                    String password, String realName) {
        // create value object
        Username usernameVO = new Username(username);
        Email emailVO = new Email(email);
        PhoneNumber phoneVO = new PhoneNumber(phoneNumber);

        // validate create policy
        userCreationPolicy.validateCreation(usernameVO, emailVO);

        // validate business rule
        userDomainService.validateUserCreation(username, email);

        // validate password policy
        passwordPolicy.validate(password);

        // password
        String encryptedPassword = userDomainService.encryptPassword(password);

        // internal method create user (skip password)
        User user = User.createWithValidatedParams(usernameVO, emailVO, phoneVO, encryptedPassword, realName);

        return user;
    }

    /**
     * from external system create user
     */
    public User createFromExternalSystem(String externalId, String username, String email, String realName) {
        // generate temporary password
        String tempPassword = userDomainService.generateDefaultPassword();

        User user = createStandardUser(username, email, tempPassword, realName);

        // set external system
        user.changeExternalId(externalId);
        user.markAsExternalUser();

        return user;
    }

    /**
     * create administrator user
     */
    public User createAdminUser(String username, String email, String password, String realName) {
        User user = createStandardUser(username, email, password, realName);
        user.grantAdminRole();
        return user;
    }
}
