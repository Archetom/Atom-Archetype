#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.context;

import ${package}.domain.context.UserContext;

/**
 * user context
 * @author hanfeng
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * set user context
     */
    public static void setContext(UserContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * get user context
     */
    public static UserContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * clear user context
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * get current user ID
     */
    public static Long getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.getCurrentUserId() : null;
    }

    /**
     * get current tenant ID
     */
    public static Long getCurrentTenantId() {
        UserContext context = getContext();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * check whether permission tenant
     */
    public static boolean canAccessTenant(Long tenantId) {
        UserContext context = getContext();
        return context != null && context.canAccessTenant(tenantId);
    }
}
