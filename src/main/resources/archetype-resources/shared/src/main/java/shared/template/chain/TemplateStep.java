package ${package}.shared.template.chain;

import ${package}.shared.template.ServiceCallback;

@FunctionalInterface
public interface TemplateStep<T> {
    void execute(ServiceCallback<T> action);
}