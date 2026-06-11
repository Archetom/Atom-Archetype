package ${package}.domain.exception;

/**
 * user domain exception
 * @author hanfeng
 */
public class UserDomainException extends DomainException {

    public UserDomainException(String message) {
        super(message);
    }

    public UserDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
