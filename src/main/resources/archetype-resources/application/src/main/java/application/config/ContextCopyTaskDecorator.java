package ${package}.application.config;

import ${package}.domain.context.UserContext;
import ${package}.domain.context.UserContextHolder;
import org.springframework.core.task.TaskDecorator;

/**
 * async context
 * <p>
 * copy of UserContext to async in,
 * async execute can current user context.
 * </p>
 * @author hanfeng
 */
public class ContextCopyTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // in in context
        UserContext context = UserContextHolder.getContext();

        return () -> {
            try {
                // in in context
                if (context != null) {
                    UserContextHolder.setContext(context);
                }
                runnable.run();
            } finally {
                // clean context, context
                UserContextHolder.clear();
            }
        };
    }
}
