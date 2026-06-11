package ${package}.domain.cache;

import java.time.Duration;

public interface CacheService {
    /**
     * get cache object
     */
    <T> T get(String key, Class<T> type);

    /**
     * cache (can)
     */
    void put(String key, Object value, Duration ttl);

    /**
     * delete cache
     */
    void evict(String key);
}
