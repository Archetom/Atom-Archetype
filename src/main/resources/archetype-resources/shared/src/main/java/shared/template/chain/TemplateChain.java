package ${package}.shared.template.chain;

import ${package}.shared.template.ServiceCallback;

import java.util.ArrayList;
import java.util.List;

public class TemplateChain<T> {
    private final List<TemplateStep<T>> steps = new ArrayList<>();

    public TemplateChain<T> addStep(TemplateStep<T> step) {
        steps.add(step);
        return this;
    }

    public T execute(ServiceCallback<T> action) {
        T result = null;
        for (TemplateStep<T> step : steps) {
            Object stepResult = step.execute(action);
            if (stepResult != null) {
                result = (T) stepResult;
            }
        }
        return result;
    }
}