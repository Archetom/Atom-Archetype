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
import ${package}.shared.context.UserContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.archetom.common.result.Pager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储实现
 * @author hanfeng
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserDao userDao;
    private final UserPOConverter userPOConverter;

    @Override
    public User save(User user) {
        UserPO userPO = userPOConverter.toPO(user);

        // 设置租户ID
        if (userPO.getTenantId() == null) {
            userPO.setTenantId(UserContextHolder.getCurrentTenantId());
        }

        if (userPO.getId() == null) {
            userDao.save(userPO);
            log.info("创建用户成功: id={}, username={}, tenantId={}",
                    userPO.getId(), userPO.getUsername(), userPO.getTenantId());
        } else {
            userDao.updateById(userPO);
            log.info("更新用户成功: id={}, username={}, tenantId={}",
                    userPO.getId(), userPO.getUsername(), userPO.getTenantId());
        }

        return userPOConverter.toDomain(userPO);
    }

    @Override
    public void delete(User user) {
        if (user != null && user.getId() != null) {
            // 检查租户权限
            checkTenantAccess(user.getId());
            userDao.removeById(user.getId());
            log.info("删除用户成功: id={}", user.getId());
        }
    }

    @Override
    public void delete(Long id) {
        if (id != null) {
            checkTenantAccess(id);
            userDao.removeById(id);
            log.info("删除用户成功: id={}", id);
        }
    }

    @Override
    public User findById(Long id) {
        if (id == null) {
            return null;
        }

        UserPO userPO = userDao.getById(id);
        if (userPO != null) {
            // 检查租户权限
            if (!canAccessTenant(userPO.getTenantId())) {
                log.warn("无权访问其他租户用户: userId={}, userTenantId={}, currentTenantId={}",
                        id, userPO.getTenantId(), UserContextHolder.getCurrentTenantId());
                return null;
            }
        }

        return userPOConverter.toDomain(userPO);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUsername, username);
        addTenantFilter(wrapper);

        UserPO userPO = userDao.getOne(wrapper);
        return Optional.ofNullable(userPOConverter.toDomain(userPO));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getEmail, email);
        addTenantFilter(wrapper);

        UserPO userPO = userDao.getOne(wrapper);
        return Optional.ofNullable(userPOConverter.toDomain(userPO));
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUsername, username);
        addTenantFilter(wrapper);

        return userDao.count(wrapper) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getEmail, email);
        addTenantFilter(wrapper);

        return userDao.count(wrapper) > 0;
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getStatus, status.getCode());
        addTenantFilter(wrapper);

        List<UserPO> userPOs = userDao.list(wrapper);
        return userPOConverter.toDomainList(userPOs);
    }

    @Override
    public Pager<User> findUsers(String username, String email, UserStatus status, int page, int size) {
        Page<UserPO> poPage = new Page<>(page, size);
        String statusCode = status != null ? status.getCode() : null;
        Long tenantId = UserContextHolder.getCurrentTenantId();

        IPage<UserPO> result = userDao.findUserPage(poPage, username, email, statusCode, tenantId);

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

    /**
     * 添加租户过滤条件
     */
    private void addTenantFilter(LambdaQueryWrapper<UserPO> wrapper) {
        Long tenantId = UserContextHolder.getCurrentTenantId();
        if (tenantId != null) {
            wrapper.eq(UserPO::getTenantId, tenantId);
        }
    }

    /**
     * 检查租户访问权限
     */
    private void checkTenantAccess(Long userId) {
        UserPO userPO = userDao.getById(userId);
        if (userPO != null && !canAccessTenant(userPO.getTenantId())) {
            throw new RuntimeException("无权访问其他租户的用户数据");
        }
    }

    /**
     * 检查是否可以访问指定租户
     */
    private boolean canAccessTenant(Long tenantId) {
        return UserContextHolder.canAccessTenant(tenantId);
    }
}
