package ${package}.domain.specification;

import ${package}.domain.entity.User;
import ${package}.domain.valueobject.TenantId;

/**
 * user specification
 * @author hanfeng
 */
public class UserSpecification {

    /**
     * user whether can
     */
    public static Specification<User> canLogin() {
        return new CompositeSpecification<User>() {
            @Override
            public boolean isSatisfiedBy(User user) {
                return user != null &&
                        user.isActive() &&
                        !user.isLocked() &&
                        !user.isDeleted();
            }
        };
    }

    /**
     * user whether can
     */
    public static Specification<User> canModify() {
        return new CompositeSpecification<User>() {
            @Override
            public boolean isSatisfiedBy(User user) {
                return user != null && !user.isDeleted();
            }
        };
    }

    /**
     * user whether in tenant
     */
    public static Specification<User> belongsToTenant(TenantId tenantId) {
        return new CompositeSpecification<User>() {
            @Override
            public boolean isSatisfiedBy(User user) {
                return user != null &&
                        user.getTenantId() != null &&
                        user.getTenantId().equals(tenantId);
            }
        };
    }

}
