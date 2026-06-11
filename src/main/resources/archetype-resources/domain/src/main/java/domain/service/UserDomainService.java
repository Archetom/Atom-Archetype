package ${package}.domain.service;

import ${package}.domain.entity.User;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.Username;

/**
 * user domain service interface
 * @author hanfeng
 */
public interface UserDomainService {

    /**
     * check username whether can
     */
    boolean isUsernameAvailable(Username username);

    /**
     * check email whether can
     */
    boolean isEmailAvailable(Email email);

    /**
     * validate user create rule
     */
    void validateUserCreation(String username, String email);

    /**
     * password
     */
    String encryptPassword(String plainPassword);

    /**
     * validate password
     */
    boolean validatePassword(String plainPassword, String encryptedPassword);

    /**
     * check user whether can delete
     */
    boolean canDeleteUser(User user);

    /**
     * generate user default password
     */
    String generateDefaultPassword();

    /**
     * check user permission
     */
    boolean hasPermission(User user, String permission);
}
