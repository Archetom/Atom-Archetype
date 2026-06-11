package ${package}.shared.lock;

import java.time.Duration;

public interface DistributedLock {
    /**
     * try get distributed lock
     * @param key locked of key
     * @param timeout etc.
     * @return whether get success
     */
    boolean tryLock(String key, Duration timeout);

    /**
     * release
     * @param key locked of key
     */
    void unlock(String key);
}
