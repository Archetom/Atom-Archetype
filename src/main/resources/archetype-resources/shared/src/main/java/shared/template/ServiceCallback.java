package ${package}.shared.template;

/**
 * 回调接口
 */
public interface ServiceCallback<T> {

    /**
     * 检查参数
     */
    default void checkParam() {
    }

    /**
     * 构建上下文
     * 目的：起到承上启下的作用，将外部参数转换为内部execute需要的参数
     */
    default void buildContext() {
    }

    /**
     * 幂等控制
     */
    default void checkConcurrent() {
    }


    /**
     * 处理业务逻辑
     *
     * @return 结果
     */
    T process();

    /**
     * 持久化
     */
    default void persistence() {
    }

    /**
     * 后置处理
     */
    default void after() {
    }
}

