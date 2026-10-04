package ${package}.application.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Map;

/**
 * Runs {@code @Async} work, such as application event listeners, on Spring Boot's application
 * task executor. Size it with {@code spring.task.execution.*}.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class TaskConfig {

    /** Spring Boot applies every {@link TaskDecorator} bean to its application task executor. */
    @Bean
    public TaskDecorator loggingContextTaskDecorator() {
        return TaskConfig::propagateLoggingContext;
    }

    /**
     * Carries the submitting thread's logging context, such as the request ID, into an async task
     * and restores the worker thread's own context afterwards.
     */
    static Runnable propagateLoggingContext(Runnable task) {
        Map<String, String> submitterContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> workerContext = MDC.getCopyOfContextMap();
            replaceLoggingContext(submitterContext);
            try {
                task.run();
            } finally {
                replaceLoggingContext(workerContext);
            }
        };
    }

    private static void replaceLoggingContext(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
