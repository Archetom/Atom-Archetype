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

    public void execute(ServiceCallback<T> action) {
        for (TemplateStep<T> step : steps) {
            step.execute(action);
        }
    }
}
