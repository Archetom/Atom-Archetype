#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.repository;

import ${package}.domain.entity.User;
import ${package}.api.enums.UserStatus;
import io.github.archetom.common.result.Pager;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口
 * @author hanfeng
 */
public interface UserRepository extends BaseRepository<User, Long> {
    
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
     * 分页查询用户
     */
    Pager<User> findUsers(String username, String email, UserStatus status, int page, int size);
}
