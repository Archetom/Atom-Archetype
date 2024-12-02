package ${package}.shared.template;

import ${package}.shared.template.thread.AppThreadLocal;
import ${package}.shared.enums.EventEnum;
import io.github.archetom.common.result.Result;

/**
 * 服务模板
 */
public interface ServiceTemplate {

    /**
     * 接口执行(框架实现)
     *
     * @param eventEnum 事件码(4位)
     * @param action    执行的接口
     * @return 返回结果
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
     * 接口执行(业务实现)
     *
     * @param eventEnum 事件码(4位)
     * @param action    执行的接口
     * @return 返回结果
     */
    <T> Result<T> doBiz(EventEnum eventEnum, ServiceCallback<T> action);


    /**
     * 开始
     * 用于上下文初始化
     */
    default void begin() {
        if (AppThreadLocal.get() == null) {
            AppThreadLocal.init();
        }
    }

    /**
     * 结束
     * 用于上下文清理
     */
    default void after() {
        AppThreadLocal.clear();
    }
}
