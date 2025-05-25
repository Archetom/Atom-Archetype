#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.mysql.dao;

import ${package}.infra.persistence.mysql.po.UserPO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 用户数据访问对象
 * @author hanfeng
 */
public interface UserDao extends BaseDao<UserPO> {

    /**
     * 根据用户名查找用户
     */
    UserPO findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    UserPO findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 分页查询用户（支持租户隔离）
     */
    IPage<UserPO> findUserPage(Page<UserPO> page, String username, String email, String status, Long tenantId);
}
