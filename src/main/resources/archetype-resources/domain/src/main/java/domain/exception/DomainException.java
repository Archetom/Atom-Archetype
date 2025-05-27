package ${package}.domain.exception;

/**
 * 领域异常基类
 * @author hanfeng
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
