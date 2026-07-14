package ${package}.domain.repository;

import ${package}.domain.entity.User;
import ${package}.domain.model.UserStatus;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;

import java.util.Optional;

/** Tenant-scoped persistence port for the User aggregate. */
public interface UserRepository {

    User save(TenantId tenantId, User user);

    Optional<User> findById(TenantId tenantId, UserId id);

    /** Return whether a username exists inside the tenant. */
    boolean existsByUsername(TenantId tenantId, String username);

    /** Return whether an email exists inside the tenant. */
    boolean existsByEmail(TenantId tenantId, String email);

    /** Query a bounded page of tenant-visible users. */
    PageResult<User> findUsers(TenantId tenantId, String username, String email, UserStatus status, int page, int size);
}
