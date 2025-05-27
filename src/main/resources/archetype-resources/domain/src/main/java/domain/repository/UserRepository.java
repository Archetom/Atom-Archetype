package ${package}.domain.repository;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.domain.valueobject.UserId;
import io.github.archetom.common.result.Pager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口
 * @author hanfeng
 */
public interface UserRepository extends BaseRepository<User, UserId> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(UserStatus status);

    /**
     * 根据租户ID查找用户
     */
    List<User> findByTenantId(Long tenantId);

    /**
     * 查找指定时间段内创建的用户
     */
    List<User> findByCreatedTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 分页查询用户
     */
    Pager<User> findUsers(String username, String email, UserStatus status, int page, int size);

    /**
     * 查找需要激活的用户（注册后未激活超过指定天数）
     */
    List<User> findUsersNeedingActivation(int days);

    /**
     * 查找长时间未登录的用户
     */
    List<User> findInactiveUsers(int days);
}
