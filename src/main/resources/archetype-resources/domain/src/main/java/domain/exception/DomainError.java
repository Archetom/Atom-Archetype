package ${package}.domain.exception;

/**
 * Stable domain failure categories independent of transport/framework codes.
 */
public enum DomainError {
    RULE_VIOLATION,
    NOT_FOUND,
    ALREADY_EXISTS,
    VERSION_CONFLICT
}
