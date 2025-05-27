package ${package}.domain.specification;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;

/**
 * 用户规约
 * @author hanfeng
 */
public class UserSpecification {

    /**
     * 用户是否可以登录
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
     * 用户是否可以修改
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
     * 用户是否属于指定租户
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
     * 用户是否为管理员
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
     * 用户是否为外部用户
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
     * 用户是否需要密码重置
     */
    public static Specification<User> needsPasswordReset() {
        return new CompositeSpecification<User>() {
            @Override
            public boolean isSatisfiedBy(User user) {
                return user != null &&
                        user.isExternalUser() &&
                        user.getPassword() != null &&
                        user.getPassword().startsWith("temp_");
            }
        };
    }

    /**
     * 组合规约示例：可以操作的用户
     */
    public static Specification<User> canBeOperatedBy(Long operatorTenantId, boolean isOperatorAdmin) {
        Specification<User> spec = canModify().and(belongsToTenant(operatorTenantId));

        if (!isOperatorAdmin) {
            // 非管理员不能操作管理员用户
            spec = spec.and(isAdmin().not());
        }

        return spec;
    }
}
