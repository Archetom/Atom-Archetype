package ${package}.infra.persistence.cache;

import ${package}.domain.cache.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component("redisCacheService")
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        // Java 8
        this.objectMapper.registerModule(new JavaTimeModule());
        // disable copy as
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Redis cache miss: {}", key);
                return null;
            }
            log.debug("Redis cache hit: {}", key);
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            log.error("Redis cache get failure: key={}", key, e);
            return null;
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (ttl != null) {
                redisTemplate.opsForValue().set(key, json, ttl);
            } else {
                redisTemplate.opsForValue().set(key, json);
            }
            log.debug("Redis cache: key={}, ttl={}", key, ttl);
        } catch (Exception e) {
            log.error("Redis cache failure: key={}", key, e);
        }
    }

    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis cache clear: {}", key);
        } catch (Exception e) {
            log.error("Redis cache clear failure: key={}", key, e);
        }
    }
}
