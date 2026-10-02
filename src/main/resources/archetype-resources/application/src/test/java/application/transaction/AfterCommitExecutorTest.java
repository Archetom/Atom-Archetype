package ${package}.application.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class AfterCommitExecutorTest {

    private final AfterCommitExecutor executor = new AfterCommitExecutor();

    @AfterEach
    void cleanupSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void defersActionUntilCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        AtomicBoolean executed = new AtomicBoolean();

        executor.execute(() -> executed.set(true));

        assertFalse(executed.get());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());
        assertTrue(executed.get());
    }

    @Test
    void runsImmediatelyWithoutTransaction() {
        AtomicBoolean executed = new AtomicBoolean();

        executor.execute(() -> executed.set(true));

        assertTrue(executed.get());
    }

    @Test
    void isolatesFailuresBetweenPostCommitActions(CapturedOutput output) {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        AtomicBoolean secondActionExecuted = new AtomicBoolean();

        executor.execute(() -> {
            throw new IllegalStateException("simulated side-effect failure for alice@example.com");
        });
        executor.execute(() -> secondActionExecuted.set(true));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());

        assertTrue(secondActionExecuted.get());
        // The failed side effect is diagnosable from its type and stack frames without its message.
        assertTrue(output.getAll().contains(IllegalStateException.class.getName()));
        assertTrue(output.getAll().contains("AfterCommitExecutorTest"));
        assertFalse(output.getAll().contains("alice@example.com"));
    }
}
