package ${package}.infra.persistence.repository;

import ${package}.domain.entity.User;
import ${package}.domain.exception.AggregateVersionConflictException;
import ${package}.domain.exception.UserAlreadyExistsException;
import ${package}.domain.model.UserStatus;
import ${package}.domain.repository.PageResult;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.specification.Specification;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import ${package}.infra.persistence.converter.UserPOConverter;
import ${package}.infra.persistence.mysql.mapper.UserMapper;
import ${package}.infra.persistence.mysql.po.UserPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Tenant-scoped user repository implementation.
 *
 * <p>Every operation requires a tenant explicitly. A missing tenant can never
 * degrade into an unscoped query.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private static final String USERNAME_UNIQUE_CONSTRAINT = "uk_t_user_tenant_username";
    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_t_user_tenant_email";

    private final UserMapper userMapper;
    private final UserPOConverter userPOConverter;

    @Override
    public User save(TenantId tenantId, User user) {
        requireTenantMatch(tenantId, user);
        UserPO userPO = userPOConverter.toPO(user);

        try {
            if (userPO.getId() == null) {
                if (userMapper.insert(userPO) != 1) {
                    throw new IllegalStateException("User insert did not affect exactly one row");
                }
                log.info("Created user: id={}, tenantId={}", userPO.getId(), tenantId.getValue());
            } else {
                LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UserPO::getId, userPO.getId())
                        .eq(UserPO::getTenantId, tenantId.getValue());
                if (userMapper.update(userPO, wrapper) != 1) {
                    throw new AggregateVersionConflictException();
                }
                log.info("Updated user: id={}, tenantId={}", userPO.getId(), tenantId.getValue());
            }
        } catch (DuplicateKeyException exception) {
            throw translateDuplicateKey(user, exception);
        }

        user.onPersisted(
                new UserId(userPO.getId()),
                userPO.getVersion(),
                userPO.getCreatedTime(),
                userPO.getUpdatedTime());
        return user;
    }

    @Override
    public List<User> saveAll(TenantId tenantId, List<User> users) {
        requireTenantId(tenantId);
        if (users == null) {
            throw new IllegalArgumentException("users must not be null");
        }
        return users.stream().map(user -> save(tenantId, user)).toList();
    }

    @Override
    public Optional<User> findById(TenantId tenantId, UserId id) {
        requireTenantId(tenantId);
        if (id == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getId, id.getValue());
        return Optional.ofNullable(userPOConverter.toDomain(userMapper.selectOne(wrapper)));
    }

    @Override
    public boolean existsById(TenantId tenantId, UserId id) {
        requireTenantId(tenantId);
        if (id == null) {
            return false;
        }
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getId, id.getValue());
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<User> findBySpecification(TenantId tenantId, Specification<User> specification) {
        requireTenantId(tenantId);
        if (specification == null) {
            throw new IllegalArgumentException("specification must not be null");
        }
        List<User> users = userPOConverter.toDomainList(userMapper.selectList(tenantFilter(tenantId)));
        return users.stream().filter(specification::isSatisfiedBy).toList();
    }

    @Override
    public Optional<User> findOneBySpecification(TenantId tenantId, Specification<User> specification) {
        return findBySpecification(tenantId, specification).stream().findFirst();
    }

    @Override
    public long countBySpecification(TenantId tenantId, Specification<User> specification) {
        return findBySpecification(tenantId, specification).size();
    }

    @Override
    public Optional<User> findByUsername(TenantId tenantId, String username) {
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getUsername, username);
        return Optional.ofNullable(userPOConverter.toDomain(userMapper.selectOne(wrapper)));
    }

    @Override
    public Optional<User> findByEmail(TenantId tenantId, String email) {
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getEmail, email);
        return Optional.ofNullable(userPOConverter.toDomain(userMapper.selectOne(wrapper)));
    }

    @Override
    public boolean existsByUsername(TenantId tenantId, String username) {
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getUsername, username);
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByEmail(TenantId tenantId, String email) {
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getEmail, email);
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<User> findByStatus(TenantId tenantId, UserStatus status) {
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getStatus, status.getCode());
        return userPOConverter.toDomainList(userMapper.selectList(wrapper));
    }

    @Override
    public List<User> findByCreatedTimeBetween(TenantId tenantId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.between(UserPO::getCreatedTime, start, end);
        return userPOConverter.toDomainList(userMapper.selectList(wrapper));
    }

    @Override
    public PageResult<User> findUsers(TenantId tenantId, String username, String email,
                                 UserStatus status, int page, int size) {
        requireTenantId(tenantId);
        Page<UserPO> poPage = new Page<>(page, size);
        String statusCode = status != null ? status.getCode() : null;
        IPage<UserPO> result = userMapper.selectUserPage(
                poPage, username, email, statusCode, tenantId.getValue());

        return new PageResult<>(
                result.getCurrent(),
                result.getSize(),
                Math.max(result.getTotal(), 0L),
                userPOConverter.toDomainList(result.getRecords()));
    }

    @Override
    public List<User> findUsersNeedingActivation(TenantId tenantId, int days) {
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getStatus, UserStatus.INACTIVE.getCode())
                .lt(UserPO::getCreatedTime, LocalDateTime.now().minusDays(days));
        return userPOConverter.toDomainList(userMapper.selectList(wrapper));
    }

    @Override
    public List<User> findInactiveUsers(TenantId tenantId, int days) {
        LambdaQueryWrapper<UserPO> wrapper = tenantFilter(tenantId);
        wrapper.eq(UserPO::getStatus, UserStatus.ACTIVE.getCode())
                .lt(UserPO::getUpdatedTime, LocalDateTime.now().minusDays(days));
        return userPOConverter.toDomainList(userMapper.selectList(wrapper));
    }

    private LambdaQueryWrapper<UserPO> tenantFilter(TenantId tenantId) {
        requireTenantId(tenantId);
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getTenantId, tenantId.getValue());
        return wrapper;
    }

    private void requireTenantMatch(TenantId tenantId, User user) {
        requireTenantId(tenantId);
        if (user == null || user.getTenantId() == null
                || !tenantId.sameValueAs(user.getTenantId())) {
            throw new IllegalArgumentException("User tenant does not match repository tenant");
        }
    }

    private void requireTenantId(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
    }

    private UserAlreadyExistsException translateDuplicateKey(User user, DuplicateKeyException exception) {
        if (causeContains(exception, EMAIL_UNIQUE_CONSTRAINT)) {
            return UserAlreadyExistsException.byEmail(user.getEmailValue());
        }
        if (causeContains(exception, USERNAME_UNIQUE_CONSTRAINT)) {
            return new UserAlreadyExistsException(user.getUsernameValue());
        }
        // The current schema has only username/email unique keys. Preserve a
        // stable conflict response if a driver omits the constraint name.
        return new UserAlreadyExistsException(user.getUsernameValue());
    }

    private boolean causeContains(Throwable exception, String fragment) {
        String expected = fragment.toLowerCase(Locale.ROOT);
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(expected)) {
                return true;
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        return false;
    }
}
