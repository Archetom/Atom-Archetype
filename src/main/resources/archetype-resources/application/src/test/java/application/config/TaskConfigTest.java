package ${package}.application.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TaskConfigTest {

    @AfterEach
    void clearLoggingContext() {
        MDC.clear();
    }

    @Test
    void asyncTaskSeesSubmitterRequestIdAndWorkerContextIsRestored() throws Exception {
        MDC.put("requestId", "request-1");
        AtomicReference<String> seenByTask = new AtomicReference<>();
        Runnable decorated = TaskConfig.propagateLoggingContext(() -> seenByTask.set(MDC.get("requestId")));
        MDC.clear();

        AtomicReference<String> afterTask = new AtomicReference<>("unset");
        Thread worker = new Thread(() -> {
            decorated.run();
            afterTask.set(MDC.get("requestId"));
        });
        worker.start();
        worker.join();

        assertEquals("request-1", seenByTask.get());
        assertNull(afterTask.get());
    }

    @Test
    void springBootTaskExecutorAppliesTheLoggingContextDecorator() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
                .withUserConfiguration(TaskConfig.class)
                .withPropertyValues("spring.task.execution.thread-name-prefix=app-task-")
                .run(context -> {
                    ThreadPoolTaskExecutor executor =
                            context.getBean("applicationTaskExecutor", ThreadPoolTaskExecutor.class);
                    MDC.put("requestId", "request-2");
                    Future<String> seen = executor.submit(
                            () -> MDC.get("requestId") + "@" + Thread.currentThread().getName());
                    MDC.clear();

                    assertEquals("request-2@app-task-1", seen.get(5, TimeUnit.SECONDS));
                });
    }
}
