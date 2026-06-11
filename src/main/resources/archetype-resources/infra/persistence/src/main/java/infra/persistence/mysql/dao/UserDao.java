#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.mysql.dao;

import ${package}.infra.persistence.mysql.po.UserPO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * user data access object
 * @author hanfeng
 */
public interface UserDao extends BaseDao<UserPO> {

    /**
     * based on username find user
     */
    UserPO findByUsername(String username);

    /**
     * based on email find user
     */
    UserPO findByEmail(String email);

    /**
     * check username whether exists
     */
    boolean existsByUsername(String username);

    /**
     * check email whether exists
     */
    boolean existsByEmail(String email);

    /**
     * paged query user (support tenant)
     */
    IPage<UserPO> findUserPage(Page<UserPO> page, String username, String email, String status, Long tenantId);
}
