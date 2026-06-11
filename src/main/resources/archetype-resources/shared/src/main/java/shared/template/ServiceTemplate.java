package ${package}.shared.template;

import ${package}.shared.template.thread.AppThreadLocal;
import ${package}.shared.enums.EventEnum;
import io.github.archetom.common.result.Result;

/**
 * service template
 */
public interface ServiceTemplate {

    /**
     * interface execute (framework implementation)
     *
     * @param eventEnum event code (4 position)
     * @param action execute of interface
     * @return return result
     */
    default <T> Result<T> execute(EventEnum eventEnum, ServiceCallback<T> action) {
        try {
            begin();
            return doBiz(eventEnum, action);
        } finally {
            after();
        }
    }

    /**
     * interface execute (business implementation)
     *
     * @param eventEnum event code (4 position)
     * @param action execute of interface
     * @return return result
     */
    <T> Result<T> doBiz(EventEnum eventEnum, ServiceCallback<T> action);


    /**
     *
     * used for context initialize
     */
    default void begin() {
        if (AppThreadLocal.get() == null) {
            AppThreadLocal.init();
        }
    }

    /**
     *
     * used for context clean
     */
    default void after() {
        AppThreadLocal.clear();
    }
}
