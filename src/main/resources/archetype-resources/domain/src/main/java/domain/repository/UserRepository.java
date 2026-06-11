package ${package}.domain.repository;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.domain.valueobject.UserId;
import io.github.archetom.common.result.Pager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * user repository interface
 * @author hanfeng
 */
public interface UserRepository extends BaseRepository<User, UserId> {

    /**
     * based on username find user
     */
    Optional<User> findByUsername(String username);

    /**
     * based on email find user
     */
    Optional<User> findByEmail(String email);

    /**
     * check username whether exists
     */
    boolean existsByUsername(String username);

    /**
     * check email whether exists
     */
    boolean existsByEmail(String email);

    /**
     * based on status find user
     */
    List<User> findByStatus(UserStatus status);

    /**
     * based on tenant ID find user
     */
    List<User> findByTenantId(Long tenantId);

    /**
     * find create of user
     */
    List<User> findByCreatedTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * paged query user
     */
    Pager<User> findUsers(String username, String email, UserStatus status, int page, int size);

    /**
     * find need active of user (inactive)
     */
    List<User> findUsersNeedingActivation(int days);

    /**
     * find not of user
     */
    List<User> findInactiveUsers(int days);
}
