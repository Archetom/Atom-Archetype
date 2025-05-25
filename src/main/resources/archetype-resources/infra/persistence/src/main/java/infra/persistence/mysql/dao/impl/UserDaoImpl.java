#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.mysql.dao.impl;

import ${package}.infra.persistence.mysql.dao.UserDao;
import ${package}.infra.persistence.mysql.mapper.UserMapper;
import ${package}.infra.persistence.mysql.po.UserPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问对象实现
 * @author hanfeng
 */
@Repository
public class UserDaoImpl extends BaseDaoImpl<UserMapper, UserPO> implements UserDao {

    @Override
    public UserPO findByUsername(String username) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUsername, username);
        return getOne(wrapper);
    }

    @Override
    public UserPO findByEmail(String email) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getEmail, email);
        return getOne(wrapper);
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUsername, username);
        return count(wrapper) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getEmail, email);
        return count(wrapper) > 0;
    }

    @Override
    public IPage<UserPO> findUserPage(Page<UserPO> page, String username, String email, String status, Long tenantId) {
        return baseMapper.selectUserPage(page, username, email, status, tenantId);
    }
}
