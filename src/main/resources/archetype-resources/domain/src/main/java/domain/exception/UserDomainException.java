package ${package}.domain.exception;

/**
 * 用户领域异常
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
