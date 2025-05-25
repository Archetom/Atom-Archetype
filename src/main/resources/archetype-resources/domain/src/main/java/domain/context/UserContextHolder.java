#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.shared.context;

import ${package}.domain.context.UserContext;

/**
 * 用户上下文持有者
 * @author hanfeng
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置用户上下文
     */
    public static void setContext(UserContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取用户上下文
     */
    public static UserContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除用户上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.getCurrentUserId() : null;
    }

    /**
     * 获取当前租户ID
     */
    public static Long getCurrentTenantId() {
        UserContext context = getContext();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * 检查是否有权限访问指定租户
     */
    public static boolean canAccessTenant(Long tenantId) {
        UserContext context = getContext();
        return context != null && context.canAccessTenant(tenantId);
    }
}
