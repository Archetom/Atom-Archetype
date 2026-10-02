package ${package}.application.service.template;

import java.util.Optional;

/**
 * Type-safe lifecycle for an application use case.
 *
 * @param <T> operation result type
 */
public interface ServiceOperation<T> {

    default void validate() {
    }

    default void prepare() {
    }

    /**
     * Returns a result that needs no database work, such as a cache hit, before any transaction
     * opens or connection is borrowed. A present result skips {@code execute} and
     * {@code onSuccess}; an empty result continues inside the template's transaction.
     */
    default Optional<T> resolveWithoutTransaction() {
        return Optional.empty();
    }

    T execute();

    default void onSuccess(T result) {
    }
}
