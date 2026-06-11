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
import ${package}.domain.context.UserContextHolder;
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
 * user repository implementation
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

        // set tenant ID
        if (userPO.getTenantId() == null) {
            userPO.setTenantId(UserContextHolder.getCurrentTenantId());
        }

        if (userPO.getId() == null) {
            userDao.save(userPO);
            log.info(" create user success: id={}, username={}, tenantId={}",
                    userPO.getId(), userPO.getUsername(), userPO.getTenantId());
        } else {
            userDao.updateById(userPO);
            log.info(" update user success: id={}, username={}, tenantId={}",
                    userPO.getId(), userPO.getUsername(), userPO.getTenantId());
        }

        // convert domain object
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
            log.info(" delete user success: id={}", id.getValue());
        }
    }

    @Override
    public Optional<User> findById(UserId id) {
        if (id == null) {
            return Optional.empty();
        }

        UserPO userPO = userDao.getById(id.getValue());
        if (userPO != null) {
            // check tenant permission
            if (!canAccessTenant(userPO.getTenantId())) {
                log.warn("No permission to access users from another tenant: userId={}, userTenantId={}, currentTenantId={}",
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
        // implementation: get all user then filter
        // actual in can copy Specification convert as database query
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

        // create new of Pager object set data
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
        // need based on actual business logic implementation
        // such as based on find not of user
        // by in current UserPO lastLoginTime field, provide implementation

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);

        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getStatus, UserStatus.ACTIVE.getCode())
                .lt(UserPO::getUpdatedTime, cutoffTime); // updated time as
        addTenantFilter(wrapper);

        List<UserPO> userPOs = userDao.list(wrapper);
        return userPOConverter.toDomainList(userPOs);
    }

    /**
     * add tenant filter
     */
    private void addTenantFilter(LambdaQueryWrapper<UserPO> wrapper) {
        Long tenantId = UserContextHolder.getCurrentTenantId();
        if (tenantId != null) {
            wrapper.eq(UserPO::getTenantId, tenantId);
        }
    }

    /**
     * check tenant permission
     */
    private void checkTenantAccess(Long userId) {
        UserPO userPO = userDao.getById(userId);
        if (userPO != null && !canAccessTenant(userPO.getTenantId())) {
            throw new RuntimeException("No permission to access user data from another tenant");
        }
    }

    /**
     * check whether can tenant
     */
    private boolean canAccessTenant(Long tenantId) {
        return UserContextHolder.canAccessTenant(tenantId);
    }
}
