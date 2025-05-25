#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.shared.lock.impl;

import ${package}.shared.lock.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地内存分布式锁实现（用于本地开发，不依赖Redis）
 * @author hanfeng
 */
@Slf4j
@Component("localMemoryDistributedLock")
public class LocalMemoryDistributedLock implements DistributedLock {

    private final ConcurrentMap<String, LockInfo> locks = new ConcurrentHashMap<>();
    private final ReentrantLock globalLock = new ReentrantLock();

    @Override
    public boolean tryLock(String key, Duration timeout) {
        globalLock.lock();
        try {
            LockInfo lockInfo = locks.get(key);
            LocalDateTime now = LocalDateTime.now();

            // 检查锁是否已过期
            if (lockInfo != null && lockInfo.expireTime.isBefore(now)) {
                locks.remove(key);
                lockInfo = null;
            }

            // 如果锁不存在或已过期，则获取锁
            if (lockInfo == null) {
                LockInfo newLockInfo = new LockInfo(
                        Thread.currentThread().getId(),
                        now.plus(timeout)
                );
                locks.put(key, newLockInfo);
                log.debug("获取本地内存锁成功: key={}, threadId={}", key, Thread.currentThread().getId());
                return true;
            } else {
                log.debug("获取本地内存锁失败，锁已被占用: key={}, ownerThreadId={}", key, lockInfo.ownerThreadId);
                return false;
            }
        } finally {
            globalLock.unlock();
        }
    }

    @Override
    public void unlock(String key) {
        globalLock.lock();
        try {
            LockInfo lockInfo = locks.get(key);
            if (lockInfo != null && lockInfo.ownerThreadId == Thread.currentThread().getId()) {
                locks.remove(key);
                log.debug("释放本地内存锁成功: key={}, threadId={}", key, Thread.currentThread().getId());
            } else {
                log.warn("尝试释放未持有的锁或锁已过期: key={}, currentThreadId={}", key, Thread.currentThread().getId());
            }
        } finally {
            globalLock.unlock();
        }
    }

    /**
     * 清理过期锁（可选的维护方法）
     */
    public void cleanExpiredLocks() {
        globalLock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            locks.entrySet().removeIf(entry -> entry.getValue().expireTime.isBefore(now));
            log.debug("清理过期锁完成，当前锁数量: {}", locks.size());
        } finally {
            globalLock.unlock();
        }
    }

    /**
     * 获取当前锁数量（用于监控）
     */
    public int getLockCount() {
        return locks.size();
    }

    /**
     * 锁信息
     */
    private static class LockInfo {
        final long ownerThreadId;
        final LocalDateTime expireTime;

        LockInfo(long ownerThreadId, LocalDateTime expireTime) {
            this.ownerThreadId = ownerThreadId;
            this.expireTime = expireTime;
        }
    }
}
