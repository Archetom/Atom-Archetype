package ${package}.application.config;

import ${package}.application.properties.TaskExecutorProperties;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

/**
 * Configures the executor used by application event listeners.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(TaskExecutorProperties.class)
public class TaskConfig {

    @Bean("taskExecutor")
    public TaskExecutor taskExecutor(TaskExecutorProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getCorePoolSize());
        executor.setMaxPoolSize(props.getMaxPoolSize());
        executor.setQueueCapacity(props.getQueueCapacity());
        executor.setKeepAliveSeconds(props.getKeepAliveSeconds());
        executor.setThreadNamePrefix(props.getThreadNamePrefix());
        executor.setTaskDecorator(TaskConfig::propagateLoggingContext);
        return executor;
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
