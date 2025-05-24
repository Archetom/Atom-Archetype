package ${package}.shared.lock;

import java.time.Duration;

public interface DistributedLock {
    /**
     * 尝试获取分布式锁
     * @param key 锁定的key
     * @param timeout 等待超时时间
     * @return 是否获取成功
     */
    boolean tryLock(String key, Duration timeout);

    /**
     * 释放锁
     * @param key 锁定的key
     */
    void unlock(String key);
}
