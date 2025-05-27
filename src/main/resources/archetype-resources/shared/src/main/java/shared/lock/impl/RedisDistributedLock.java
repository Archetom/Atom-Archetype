#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.shared.lock.impl;

import ${package}.shared.lock.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Redis 分布式锁实现
 * @author hanfeng
 */
@Slf4j
@Component("redisDistributedLock")
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisDistributedLock implements DistributedLock {

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentMap<String, String> lockValues = new ConcurrentHashMap<>();

    private static final String LOCK_PREFIX = "lock:";
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(String key, Duration timeout) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();

        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, timeout);

            if (Boolean.TRUE.equals(success)) {
                lockValues.put(key, lockValue);
                log.debug("获取Redis分布式锁成功: {}", key);
                return true;
            } else {
                log.debug("获取Redis分布式锁失败: {}", key);
                return false;
            }
        } catch (Exception e) {
            log.error("获取Redis分布式锁异常: {}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = lockValues.get(key);

        if (lockValue == null) {
            log.warn("尝试释放未持有的Redis锁: {}", key);
            return;
        }

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(UNLOCK_SCRIPT);
            script.setResultType(Long.class);

            Long result = redisTemplate.execute(script,
                    Collections.singletonList(lockKey), lockValue);

            if (result != null && result == 1) {
                lockValues.remove(key);
                log.debug("释放Redis分布式锁成功: {}", key);
            } else {
                log.warn("释放Redis分布式锁失败，锁可能已过期: {}", key);
                lockValues.remove(key); // 清理本地记录
            }
        } catch (Exception e) {
            log.error("释放Redis分布式锁异常: {}", key, e);
            lockValues.remove(key); // 清理本地记录
        }
    }
}
