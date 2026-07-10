package ${package}.infra.persistence.lock;

import ${package}.application.port.out.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis lock whose ownership token lives in the returned handle.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "atom.redis.enabled", havingValue = "true")
public class RedisDistributedLock implements DistributedLock {

    private static final String LOCK_PREFIX = "lock:";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisDistributedLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<LockHandle> tryLock(String key, Duration leaseTime) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("lock key must not be blank");
        }
        if (leaseTime == null || leaseTime.isZero() || leaseTime.isNegative()) {
            throw new IllegalArgumentException("leaseTime must be positive");
        }

        String redisKey = LOCK_PREFIX + key;
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, token, leaseTime);
            return Boolean.TRUE.equals(acquired)
                    ? Optional.of(new RedisLockHandle(key, redisKey, token))
                    : Optional.empty();
        } catch (RuntimeException exception) {
            log.error("Redis lock acquisition failed: exceptionType={}", exception.getClass().getName());
            return Optional.empty();
        }
    }

    private final class RedisLockHandle implements LockHandle {
        private final String key;
        private final String redisKey;
        private final String token;
        private final AtomicBoolean released = new AtomicBoolean();

        private RedisLockHandle(String key, String redisKey, String token) {
            this.key = key;
            this.redisKey = redisKey;
            this.token = token;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public boolean release() {
            if (!released.compareAndSet(false, true)) {
                return false;
            }
            try {
                Long result = redisTemplate.execute(
                        RELEASE_SCRIPT, Collections.singletonList(redisKey), token);
                return Long.valueOf(1L).equals(result);
            } catch (RuntimeException exception) {
                log.error("Redis lock release failed: exceptionType={}", exception.getClass().getName());
                return false;
            }
        }
    }
}
