package ${package}.domain.exception;

/**
 * Raised when an aggregate was changed by another transaction.
 */
public class AggregateVersionConflictException extends DomainException {

    public AggregateVersionConflictException() {
        super(DomainError.VERSION_CONFLICT, "Resource version conflict");
    }
}
