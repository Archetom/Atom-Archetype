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
        // 注册 Java 8 时间模块
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Redis缓存未命中: {}", key);
                return null;
            }
            log.debug("Redis缓存命中: {}", key);
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            log.error("Redis缓存获取失败: key={}", key, e);
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
            log.debug("Redis缓存存储: key={}, ttl={}", key, ttl);
        } catch (Exception e) {
            log.error("Redis缓存存储失败: key={}", key, e);
        }
    }

    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis缓存清除: {}", key);
        } catch (Exception e) {
            log.error("Redis缓存清除失败: key={}", key, e);
        }
    }
}
