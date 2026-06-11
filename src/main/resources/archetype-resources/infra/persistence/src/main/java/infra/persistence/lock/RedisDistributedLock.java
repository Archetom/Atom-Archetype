#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.lock;

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
 * Redis distributed lock implementation
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
                log.debug(" get Redis distributed lock success: {}", key);
                return true;
            } else {
                log.debug(" get Redis distributed lock failure: {}", key);
                return false;
            }
        } catch (Exception e) {
            log.error(" get Redis distributed lock exception: {}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = lockValues.get(key);

        if (lockValue == null) {
            log.warn(" try release not of Redis: {}", key);
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
                log.debug(" release Redis distributed lock success: {}", key);
            } else {
                log.warn(" release Redis distributed lock failure, can already: {}", key);
                lockValues.remove(key); // clean
            }
        } catch (Exception e) {
            log.error(" release Redis distributed lock exception: {}", key, e);
            lockValues.remove(key); // clean
        }
    }
}
