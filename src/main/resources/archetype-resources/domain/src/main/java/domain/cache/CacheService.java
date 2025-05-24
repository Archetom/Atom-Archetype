package ${package}.domain.cache;

import java.time.Duration;

public interface CacheService {
    /**
     * 获取缓存对象
     */
    <T> T get(String key, Class<T> type);

    /**
     * 写入缓存（可指定过期时间）
     */
    void put(String key, Object value, Duration ttl);

    /**
     * 删除缓存
     */
    void evict(String key);
}
