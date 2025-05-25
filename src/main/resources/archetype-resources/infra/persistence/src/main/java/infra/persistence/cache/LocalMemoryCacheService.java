#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.cache;

import ${package}.domain.cache.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 本地内存缓存服务实现（用于本地开发，不依赖Redis）
 * @author hanfeng
 */
@Slf4j
@Component("localMemoryCacheService")
public class LocalMemoryCacheService implements CacheService {

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, CacheItem> cache = new ConcurrentHashMap<>();

    public LocalMemoryCacheService() {
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 时间模块
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        CacheItem item = cache.get(key);
        if (item == null) {
            log.debug("本地缓存未命中: {}", key);
            return null;
        }

        // 检查是否过期
        if (item.expireTime != null && item.expireTime.isBefore(LocalDateTime.now())) {
            cache.remove(key);
            log.debug("本地缓存已过期: {}", key);
            return null;
        }

        try {
            log.debug("本地缓存命中: {}", key);
            return objectMapper.readValue(item.value, type);
        } catch (Exception e) {
            log.error("本地缓存反序列化失败: key={}", key, e);
            cache.remove(key);
            return null;
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            LocalDateTime expireTime = ttl != null ? LocalDateTime.now().plus(ttl) : null;
            cache.put(key, new CacheItem(json, expireTime));
            log.debug("本地缓存存储: key={}, ttl={}", key, ttl);
        } catch (Exception e) {
            log.error("本地缓存序列化失败: key={}", key, e);
        }
    }

    @Override
    public void evict(String key) {
        cache.remove(key);
        log.debug("本地缓存清除: {}", key);
    }

    /**
     * 清理过期缓存（可选的维护方法）
     */
    public void cleanExpiredCache() {
        LocalDateTime now = LocalDateTime.now();
        cache.entrySet().removeIf(entry ->
                entry.getValue().expireTime != null && entry.getValue().expireTime.isBefore(now));
        log.debug("清理过期缓存完成，当前缓存数量: {}", cache.size());
    }

    /**
     * 获取当前缓存数量（用于监控）
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 缓存项
     */
    private static class CacheItem {
        final String value;
        final LocalDateTime expireTime;

        CacheItem(String value, LocalDateTime expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
    }
}
