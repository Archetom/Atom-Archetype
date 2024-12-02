package ${package}.shared.template.chain;

import io.github.archetom.common.utils.Profiler;

public class StandardTemplateSteps {
    /**
     * 参数校验步骤
     */
    public static <T> TemplateStep<T> checkParam() {
        return action -> {
            Profiler.enter("checkParam");
            action.checkParam();
            Profiler.release();
        };
    }

    /**
     * 构建上下文步骤
     */
    public static <T> TemplateStep<T> buildContext() {
        return action -> {
            Profiler.enter("buildContext");
            action.buildContext();
            Profiler.release();
        };
    }

    /**
     * 幂等校验步骤
     */
    public static <T> TemplateStep<T> checkConcurrent() {
        return action -> {
            Profiler.enter("checkConcurrent");
            action.checkConcurrent();
            Profiler.release();
        };
    }

    /**
     * 核心处理逻辑
     */
    public static <T> TemplateStep<T> process() {
        return action -> {
            Profiler.enter("process");
            action.process();
            Profiler.release();
        };
    }

    /**
     * 持久化步骤
     */
    public static <T> TemplateStep<T> persistence() {
        return action -> {
            Profiler.enter("persistence");
            action.persistence();
            Profiler.release();
        };
    }

    /**
     * 后置处理步骤
     */
    public static <T> TemplateStep<T> after() {
        return action -> {
            Profiler.enter("after");
            action.after();
            Profiler.release();
        };
    }
}
