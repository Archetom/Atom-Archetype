#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.repository;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.domain.repository.UserRepository;
import ${package}.infra.persistence.converter.UserPOConverter;
import ${package}.infra.persistence.mysql.dao.UserDao;
import ${package}.infra.persistence.mysql.po.UserPO;
import ${package}.infra.persistence.mysql.util.PageUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.archetom.common.result.Pager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储实现
 * @author hanfeng
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    
    private final UserDao userDao;

    private final UserPOConverter userPOConverter;

    @Override
    public User save(User user) {
        UserPO userPO = userPOConverter.toPO(user);

        if (userPO.getId() == null) {
            userDao.save(userPO);
        } else {
            userDao.updateById(userPO);
        }

        return userPOConverter.toDomain(userPO);
    }
    
    @Override
    public void delete(User user) {
        if (user != null && user.getId() != null) {
            userDao.removeById(user.getId());
        }
    }
    
    @Override
    public void delete(Long id) {
        if (id != null) {
            userDao.removeById(id);
        }
    }

    @Override
    public User findById(Long id) {
        if (id == null) {
            return null;
        }

        UserPO userPO = userDao.getById(id);
        return userPOConverter.toDomain(userPO);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserPO userPO = userDao.findByUsername(username);
        return Optional.ofNullable(userPOConverter.toDomain(userPO));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        UserPO userPO = userDao.findByEmail(email);
        return Optional.ofNullable(userPOConverter.toDomain(userPO));
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userDao.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userDao.existsByEmail(email);
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getStatus, status.getCode());

        List<UserPO> userPOs = userDao.list(wrapper);
        return userPOConverter.toDomainList(userPOs);
    }

    @Override
    public Pager<User> findUsers(String username, String email, UserStatus status, int page, int size) {
        Page<UserPO> poPage = new Page<>(page, size);
        String statusCode = status != null ? status.getCode() : null;

        IPage<UserPO> result = userDao.findUserPage(poPage, username, email, statusCode);

        Pager<User> pager = PageUtil.toPager(result);
        List<User> users = userPOConverter.toDomainList(result.getRecords());

        // 创建新的Pager对象并设置数据
        Pager<User> finalPager = new Pager<>();
        finalPager.setPageNum(pager.getPageNum());
        finalPager.setPageSize(pager.getPageSize());
        finalPager.setTotalNum(pager.getTotalNum());
        finalPager.setObjectList(users);

        return finalPager;
    }
}
