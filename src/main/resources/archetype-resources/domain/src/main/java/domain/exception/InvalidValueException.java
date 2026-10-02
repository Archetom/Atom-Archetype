package ${package}.domain.exception;

/**
 * Raised when a value object rejects its input.
 *
 * <p>Value validation is a client-correctable domain failure. Keep messages free of the
 * rejected value; they may be returned to callers. Programming errors and broken
 * invariants must use other exception types so they surface as internal failures.</p>
 */
public class InvalidValueException extends DomainException {

    public InvalidValueException(String message) {
        super(DomainError.INVALID_VALUE, message);
    }
}
