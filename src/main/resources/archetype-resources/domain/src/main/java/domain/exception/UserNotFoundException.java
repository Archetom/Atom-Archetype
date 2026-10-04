package ${package}.domain.exception;

/**
 * Raised when a user does not exist, is deleted, or belongs to another tenant.
 *
 * <p>All three cases share one message so callers cannot probe other tenants.</p>
 */
public class UserNotFoundException extends UserDomainException {

    public UserNotFoundException() {
        super(DomainError.NOT_FOUND, "User does not exist");
    }
}
