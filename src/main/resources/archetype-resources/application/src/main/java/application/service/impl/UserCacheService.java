#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application.service.impl;

import ${package}.application.vo.UserVO;
import ${package}.domain.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 用户缓存服务
 * @author hanfeng
 */
@Slf4j
@Service
public class UserCacheService {

    private final CacheService cacheService;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    public UserCacheService(@Qualifier("localMemoryCacheService") CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 缓存用户信息
     */
    public void cacheUser(UserVO user) {
        if (user != null && user.getId() != null) {
            String cacheKey = USER_CACHE_PREFIX + user.getId();
            cacheService.put(cacheKey, user, USER_CACHE_TTL);
            log.debug("缓存用户信息: userId={}", user.getId());
        }
    }

    /**
     * 获取缓存的用户信息
     */
    public UserVO getCachedUser(Long userId) {
        if (userId == null) {
            return null;
        }

        String cacheKey = USER_CACHE_PREFIX + userId;
        UserVO user = cacheService.get(cacheKey, UserVO.class);

        if (user != null) {
            log.debug("命中用户缓存: userId={}", userId);
        } else {
            log.debug("未命中用户缓存: userId={}", userId);
        }

        return user;
    }

    /**
     * 清除用户缓存
     */
    public void evictUser(Long userId) {
        if (userId != null) {
            String cacheKey = USER_CACHE_PREFIX + userId;
            cacheService.evict(cacheKey);
            log.debug("清除用户缓存: userId={}", userId);
        }
    }

    /**
     * 缓存用户名到ID的映射
     */
    public void cacheUsernameMapping(String username, Long userId) {
        if (username != null && userId != null) {
            String cacheKey = "username:" + username;
            cacheService.put(cacheKey, userId, USER_CACHE_TTL);
            log.debug("缓存用户名映射: username={}, userId={}", username, userId);
        }
    }

    /**
     * 获取用户名对应的用户ID
     */
    public Long getCachedUserIdByUsername(String username) {
        if (username == null) {
            return null;
        }

        String cacheKey = "username:" + username;
        return cacheService.get(cacheKey, Long.class);
    }
}
