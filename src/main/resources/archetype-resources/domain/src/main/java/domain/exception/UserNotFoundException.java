package ${package}.domain.exception;

/**
 * user not to exception
 * @author hanfeng
 */
public class UserNotFoundException extends UserDomainException {

    public UserNotFoundException(Long userId) {
        super("User does not exist: " + userId);
    }

    public UserNotFoundException(String username) {
        super("User does not exist: " + username);
    }
}