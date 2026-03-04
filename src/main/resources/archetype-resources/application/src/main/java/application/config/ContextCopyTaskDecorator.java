package ${package}.application.config;

import ${package}.domain.context.UserContext;
import ${package}.domain.context.UserContextHolder;
import org.springframework.core.task.TaskDecorator;

/**
 * 异步任务上下文传播装饰器
 * <p>
 * 将调用线程的 UserContext 复制到异步任务线程中，
 * 确保异步执行时仍可访问当前用户上下文。
 * </p>
 * @author hanfeng
 */
public class ContextCopyTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 在调用线程中捕获上下文
        UserContext context = UserContextHolder.getContext();

        return () -> {
            try {
                // 在任务线程中恢复上下文
                if (context != null) {
                    UserContextHolder.setContext(context);
                }
                runnable.run();
            } finally {
                // 任务完成后清理上下文，防止线程池线程复用时上下文泄漏
                UserContextHolder.clear();
            }
        };
    }
}
