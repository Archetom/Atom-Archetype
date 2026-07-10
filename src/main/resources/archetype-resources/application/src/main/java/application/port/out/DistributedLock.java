package ${package}.application.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Output port for cross-process mutual exclusion.
 */
public interface DistributedLock {

    Optional<LockHandle> tryLock(String key, Duration leaseTime);

    interface LockHandle extends AutoCloseable {

        String key();

        boolean release();

        @Override
        default void close() {
            release();
        }
    }
}
