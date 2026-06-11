package ${package}.domain.specification;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;

/**
 * user specification
 * @author hanfeng
 */
public class UserSpecification {

    private static final String TEMP_PASSWORD_PREFIX = "temp_";

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
    public static Specification<User> belongsToTenant(Long tenantId) {
        return new CompositeSpecification<User>() {
            @Override
            public boolean isSatisfiedBy(User user) {
                return user != null &&
                        user.getTenantId() != null &&
                        user.getTenantId().equals(tenantId);
            }
        };
    }

    /**
     * user whether as administrator
     */
    public static Specification<User> isAdmin() {
        return new CompositeSpecification<User>() {
            @Override
            public boolean isSatisfiedBy(User user) {
                return user != null && user.isAdmin();
            }
        };
    }

    /**
     * user whether as External User
     */
    public static Specification<User> isExternalUser() {
        return new CompositeSpecification<User>() {
            @Override
            public boolean isSatisfiedBy(User user) {
                return user != null && user.isExternalUser();
            }
        };
    }

    /**
     * user whether need password
     */
    public static Specification<User> needsPasswordReset() {
        return new CompositeSpecification<User>() {
            @Override
            public boolean isSatisfiedBy(User user) {
                return user != null &&
                        user.isExternalUser() &&
                        user.getPassword() != null &&
                        user.getPassword().startsWith(TEMP_PASSWORD_PREFIX);
            }
        };
    }

    /**
     * specification sample: can of user
     */
    public static Specification<User> canBeOperatedBy(Long operatorTenantId, boolean isOperatorAdmin) {
        Specification<User> spec = canModify().and(belongsToTenant(operatorTenantId));

        if (!isOperatorAdmin) {
            // non- administrator cannot administrator user
            spec = spec.and(isAdmin().not());
        }

        return spec;
    }
}
