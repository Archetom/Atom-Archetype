package ${package}.application.service.impl;

import ${package}.application.port.out.CacheStore;
import ${package}.application.vo.UserVO;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Tenant-scoped user cache facade.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    private final CacheStore cacheStore;

    public void cacheUser(TenantId tenantId, UserVO user) {
        requireTenantId(tenantId);
        if (user == null || user.getId() == null) {
            return;
        }
        if (!tenantId.getValue().equals(user.getTenantId())) {
            throw new IllegalArgumentException("Cached user tenant does not match cache tenant");
        }
        cacheStore.put(userKey(tenantId, new UserId(user.getId())), user, USER_CACHE_TTL);
    }

    public UserVO getCachedUser(TenantId tenantId, UserId userId) {
        requireTenantId(tenantId);
        if (userId == null) {
            return null;
        }
        String key = userKey(tenantId, userId);
        UserVO user = cacheStore.get(key, UserVO.class);
        if (user != null && !tenantId.getValue().equals(user.getTenantId())) {
            cacheStore.evict(key);
            log.warn("Rejected user cache entry with mismatched tenant: userId={}", userId.getValue());
            return null;
        }
        return user;
    }

    public void evictUser(TenantId tenantId, UserId userId) {
        requireTenantId(tenantId);
        if (userId != null) {
            cacheStore.evict(userKey(tenantId, userId));
        }
    }

    public void cacheUsernameMapping(TenantId tenantId, String username, UserId userId) {
        requireTenantId(tenantId);
        if (username != null && userId != null) {
            cacheStore.put(usernameKey(tenantId, username), userId.getValue(), USER_CACHE_TTL);
        }
    }

    public Long getCachedUserIdByUsername(TenantId tenantId, String username) {
        requireTenantId(tenantId);
        if (username == null) {
            return null;
        }
        return cacheStore.get(usernameKey(tenantId, username), Long.class);
    }

    private String userKey(TenantId tenantId, UserId userId) {
        return "tenant:%d:user:%d".formatted(tenantId.getValue(), userId.getValue());
    }

    private String usernameKey(TenantId tenantId, String username) {
        return "tenant:%d:username:%s".formatted(tenantId.getValue(), username);
    }

    private void requireTenantId(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
    }
}
