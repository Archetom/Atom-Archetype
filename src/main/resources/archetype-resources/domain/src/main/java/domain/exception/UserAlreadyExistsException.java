package ${package}.domain.exception;

/**
 * user already exists exception
 * @author hanfeng
 */
public class UserAlreadyExistsException extends UserDomainException {

    public UserAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }

    public static UserAlreadyExistsException byEmail(String email) {
        return new UserAlreadyExistsException("Email already exists: " + email);
    }
}
