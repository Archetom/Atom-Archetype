package ${package}.shared.template.chain;

import io.github.archetom.common.utils.Profiler;

public class StandardTemplateSteps {
    /**
     * parameter validation
     */
    public static <T> TemplateStep<T> checkParam() {
        return action -> {
            Profiler.enter("checkParam");
            action.checkParam();
            Profiler.release();

            return null;
        };
    }

    /**
     * build context
     */
    public static <T> TemplateStep<T> buildContext() {
        return action -> {
            Profiler.enter("buildContext");
            action.buildContext();
            Profiler.release();

            return null;
        };
    }

    /**
     * idempotency
     */
    public static <T> TemplateStep<T> checkConcurrent() {
        return action -> {
            Profiler.enter("checkConcurrent");
            action.checkConcurrent();
            Profiler.release();

            return null;
        };
    }

    /**
     * core processing logic
     */
    public static <T> TemplateStep<T> process() {
        return action -> {
            Profiler.enter("process");
            T result = action.process(); // get process result
            Profiler.release();

            return result; // return process result
        };
    }

    /**
     * persistence
     */
    public static <T> TemplateStep<T> persistence() {
        return action -> {
            Profiler.enter("persistence");
            action.persistence();
            Profiler.release();

            return null;
        };
    }

    /**
     * post-processing
     */
    public static <T> TemplateStep<T> after() {
        return action -> {
            Profiler.enter("after");
            action.after();
            Profiler.release();

            return null;
        };
    }
}
