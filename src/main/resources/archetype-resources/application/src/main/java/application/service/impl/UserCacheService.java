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
 * user cache service
 * @author hanfeng
 */
@Slf4j
@Service
public class UserCacheService {

    private final CacheService cacheService;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    public UserCacheService(@Qualifier("redisCacheService") CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * cache user
     */
    public void cacheUser(UserVO user) {
        if (user != null && user.getId() != null) {
            String cacheKey = USER_CACHE_PREFIX + user.getId();
            cacheService.put(cacheKey, user, USER_CACHE_TTL);
            log.debug(" cache user: userId={}", user.getId());
        }
    }

    /**
     * get cache of user
     */
    public UserVO getCachedUser(Long userId) {
        if (userId == null) {
            return null;
        }

        String cacheKey = USER_CACHE_PREFIX + userId;
        UserVO user = cacheService.get(cacheKey, UserVO.class);

        if (user != null) {
            log.debug(" hit user cache: userId={}", userId);
        } else {
            log.debug(" miss user cache: userId={}", userId);
        }

        return user;
    }

    /**
     * clear user cache
     */
    public void evictUser(Long userId) {
        if (userId != null) {
            String cacheKey = USER_CACHE_PREFIX + userId;
            cacheService.evict(cacheKey);
            log.debug(" clear user cache: userId={}", userId);
        }
    }

    /**
     * cache username to ID of mapping
     */
    public void cacheUsernameMapping(String username, Long userId) {
        if (username != null && userId != null) {
            String cacheKey = "username:" + username;
            cacheService.put(cacheKey, userId, USER_CACHE_TTL);
            log.debug(" cache username mapping: username={}, userId={}", username, userId);
        }
    }

    /**
     * get username for of user ID
     */
    public Long getCachedUserIdByUsername(String username) {
        if (username == null) {
            return null;
        }

        String cacheKey = "username:" + username;
        return cacheService.get(cacheKey, Long.class);
    }
}
