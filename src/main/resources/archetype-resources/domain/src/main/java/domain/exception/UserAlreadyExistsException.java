package ${package}.domain.exception;

/**
 * Raised when another user in the tenant already holds the username or email.
 *
 * <p>The message names only the conflicting field, never its value.</p>
 */
public class UserAlreadyExistsException extends UserDomainException {

    private UserAlreadyExistsException(String message) {
        super(DomainError.ALREADY_EXISTS, message);
    }

    public static UserAlreadyExistsException byUsername() {
        return new UserAlreadyExistsException("Username already exists");
    }

    public static UserAlreadyExistsException byEmail() {
        return new UserAlreadyExistsException("Email already exists");
    }
}
