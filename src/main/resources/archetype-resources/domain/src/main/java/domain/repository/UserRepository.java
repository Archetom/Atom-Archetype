package ${package}.domain.repository;

import ${package}.domain.entity.User;
import ${package}.domain.model.UserStatus;
import ${package}.domain.specification.Specification;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Tenant-scoped persistence port for the User aggregate. */
public interface UserRepository {

    User save(TenantId tenantId, User user);

    List<User> saveAll(TenantId tenantId, List<User> users);

    Optional<User> findById(TenantId tenantId, UserId id);

    boolean existsById(TenantId tenantId, UserId id);

    List<User> findBySpecification(TenantId tenantId, Specification<User> specification);

    Optional<User> findOneBySpecification(TenantId tenantId, Specification<User> specification);

    long countBySpecification(TenantId tenantId, Specification<User> specification);

    /** Find a user by tenant-scoped username. */
    Optional<User> findByUsername(TenantId tenantId, String username);

    /** Find a user by tenant-scoped email. */
    Optional<User> findByEmail(TenantId tenantId, String email);

    /** Return whether a username exists inside the tenant. */
    boolean existsByUsername(TenantId tenantId, String username);

    /** Return whether an email exists inside the tenant. */
    boolean existsByEmail(TenantId tenantId, String email);

    /** Find users with the requested status inside the tenant. */
    List<User> findByStatus(TenantId tenantId, UserStatus status);

    /** Find users created inside a time interval. */
    List<User> findByCreatedTimeBetween(TenantId tenantId, LocalDateTime start, LocalDateTime end);

    /** Query a bounded page of tenant-visible users. */
    PageResult<User> findUsers(TenantId tenantId, String username, String email, UserStatus status, int page, int size);

    /** Find inactive users old enough for an activation workflow. */
    List<User> findUsersNeedingActivation(TenantId tenantId, int days);

    /** Find active users that have not changed during the supplied number of days. */
    List<User> findInactiveUsers(TenantId tenantId, int days);
}
