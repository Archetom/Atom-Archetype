package ${package}.infra.persistence.repository;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.specification.Specification;
import ${package}.domain.valueobject.UserId;
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

import java.time.LocalDateTime;
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

        // 转换回领域对象
        User savedUser = userPOConverter.toDomain(userPO);
        return savedUser;
    }

    @Override
    public List<User> saveAll(List<User> users) {
        List<UserPO> userPOs = userPOConverter.toPOList(users);
        userDao.saveBatch(userPOs);
        return userPOConverter.toDomainList(userPOs);
    }

    @Override
    public void delete(User user) {
        if (user != null && user.getId() != null) {
            deleteById(user.getId());
        }
    }

    @Override
    public void deleteById(UserId id) {
        if (id != null) {
            checkTenantAccess(id.getValue());
            userDao.removeById(id.getValue());
            log.info("删除用户成功: id={}", id.getValue());
        }
    }

    @Override
    public Optional<User> findById(UserId id) {
        if (id == null) {
            return Optional.empty();
        }

        UserPO userPO = userDao.getById(id.getValue());
        if (userPO != null) {
            // 检查租户权限
            if (!canAccessTenant(userPO.getTenantId())) {
                log.warn("无权访问其他租户用户: userId={}, userTenantId={}, currentTenantId={}",
                        id.getValue(), userPO.getTenantId(), UserContextHolder.getCurrentTenantId());
                return Optional.empty();
            }

            User user = userPOConverter.toDomain(userPO);
            return Optional.of(user);
        }

        return Optional.empty();
    }

    @Override
    public boolean existsById(UserId id) {
        if (id == null) {
            return false;
        }

        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getId, id.getValue());
        addTenantFilter(wrapper);

        return userDao.count(wrapper) > 0;
    }

    @Override
    public List<User> findBySpecification(Specification<User> specification) {
        // 简化实现：获取所有用户然后过滤
        // 实际项目中可以将 Specification 转换为数据库查询条件
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        addTenantFilter(wrapper);

        List<UserPO> userPOs = userDao.list(wrapper);
        List<User> users = userPOConverter.toDomainList(userPOs);

        return users.stream()
                .filter(specification::isSatisfiedBy)
                .toList();
    }

    @Override
    public Optional<User> findOneBySpecification(Specification<User> specification) {
        List<User> users = findBySpecification(specification);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public long countBySpecification(Specification<User> specification) {
        return findBySpecification(specification).size();
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
    public List<User> findByTenantId(Long tenantId) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getTenantId, tenantId);

        List<UserPO> userPOs = userDao.list(wrapper);
        return userPOConverter.toDomainList(userPOs);
    }

    @Override
    public List<User> findByCreatedTimeBetween(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(UserPO::getCreatedTime, start, end);
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

    @Override
    public List<User> findUsersNeedingActivation(int days) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getStatus, UserStatus.INACTIVE.getCode())
                .lt(UserPO::getCreatedTime, cutoffTime);
        addTenantFilter(wrapper);

        List<UserPO> userPOs = userDao.list(wrapper);
        return userPOConverter.toDomainList(userPOs);
    }

    @Override
    public List<User> findInactiveUsers(int days) {
        // 这里需要根据实际业务逻辑实现
        // 比如根据最后登录时间查找长时间未登录的用户
        // 由于当前 UserPO 没有 lastLoginTime 字段，这里提供一个简化实现

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getStatus, UserStatus.ACTIVE.getCode())
                .lt(UserPO::getUpdatedTime, cutoffTime); // 使用更新时间作为活跃度指标
        addTenantFilter(wrapper);

        List<UserPO> userPOs = userDao.list(wrapper);
        return userPOConverter.toDomainList(userPOs);
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
