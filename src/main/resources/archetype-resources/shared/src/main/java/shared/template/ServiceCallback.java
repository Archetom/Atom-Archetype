package ${package}.shared.template;

/**
 * callback interface
 */
public interface ServiceCallback<T> {

    /**
     * check parameter
     */
    default void checkParam() {
    }

    /**
     * build context
     * of: to of, copy external parameter convert as internal execute need of parameter
     */
    default void buildContext() {
    }

    /**
     * idempotency control
     */
    default void checkConcurrent() {
    }


    /**
     * process business logic
     *
     * @return result
     */
    T process();

    /**
     * persistence
     */
    default void persistence() {
    }

    /**
     * post-processing
     */
    default void after() {
    }
}

