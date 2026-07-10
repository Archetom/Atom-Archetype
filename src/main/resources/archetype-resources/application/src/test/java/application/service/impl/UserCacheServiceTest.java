package ${package}.application.service.impl;

import ${package}.application.port.out.CacheStore;
import ${package}.application.vo.UserVO;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserCacheServiceTest {

    private final InMemoryCache cache = new InMemoryCache();
    private final UserCacheService service = new UserCacheService(cache);

    @Test
    void isolatesIdenticalUserIdsByTenant() {
        TenantId tenantA = new TenantId(1L);
        TenantId tenantB = new TenantId(2L);
        UserId userId = new UserId(100L);

        service.cacheUser(tenantA, user(100L, 1L));

        assertNotNull(service.getCachedUser(tenantA, userId));
        assertNull(service.getCachedUser(tenantB, userId));
    }

    @Test
    void rejectsAndEvictsPayloadWithWrongTenant() {
        TenantId tenant = new TenantId(1L);
        UserId userId = new UserId(100L);
        String key = "tenant:1:user:100";
        cache.put(key, user(100L, 2L), Duration.ofMinutes(1));

        assertNull(service.getCachedUser(tenant, userId));
        assertFalse(cache.values.containsKey(key));
    }

    @Test
    void tenantScopesUsernameMappings() {
        service.cacheUsernameMapping(new TenantId(1L), "alice", new UserId(10L));

        assertEquals(10L, service.getCachedUserIdByUsername(new TenantId(1L), "alice"));
        assertNull(service.getCachedUserIdByUsername(new TenantId(2L), "alice"));
    }

    private UserVO user(Long id, Long tenantId) {
        return new UserVO().setId(id).setTenantId(tenantId).setUsername("alice");
    }

    private static final class InMemoryCache implements CacheStore {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public <T> T get(String key, Class<T> type) {
            Object value = values.get(key);
            return value == null ? null : type.cast(value);
        }

        @Override
        public void put(String key, Object value, Duration ttl) {
            values.put(key, value);
        }

        @Override
        public void evict(String key) {
            values.remove(key);
        }
    }
}
