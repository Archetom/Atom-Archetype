package ${package}.application.transaction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs side effects only after the surrounding transaction has committed.
 */
@Slf4j
@Component
public class AfterCommitExecutor {

    public void execute(Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            runSafely(action);
                        }
                    });
            return;
        }

        runSafely(action);
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            // The database commit has already succeeded. Record the failure for
            // retry/alerting instead of reporting the business transaction as failed.
            log.error("Post-commit action failed: exceptionType={}", exception.getClass().getName());
        }
    }
}
