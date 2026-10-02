package ${package}.application.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

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
}
