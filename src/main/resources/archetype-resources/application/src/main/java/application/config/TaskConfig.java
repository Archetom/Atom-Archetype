package ${package}.application.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池配置，支持 yml 动态调整参数
 * author: hanfeng
 */
@Configuration
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
        executor.initialize();
        return executor;
    }
}
