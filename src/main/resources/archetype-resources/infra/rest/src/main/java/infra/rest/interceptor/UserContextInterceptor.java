#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.rest.interceptor;

import ${package}.domain.context.UserContext;
import ${package}.shared.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 用户上下文拦截器
 * @author hanfeng
 */
@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头中获取用户信息（实际项目中可能从JWT token中解析）
        String userIdHeader = request.getHeader("X-User-Id");
        String tenantIdHeader = request.getHeader("X-Tenant-Id");
        String requestId = request.getHeader("X-Request-Id");

        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        // 创建用户上下文
        UserContext context = UserContext.create(
                userIdHeader != null ? Long.parseLong(userIdHeader) : 1L, // 默认用户ID为1
                tenantIdHeader != null ? Long.parseLong(tenantIdHeader) : 1L // 默认租户ID为1
        ).withOperation(
                request.getMethod() + " " + request.getRequestURI(),
                getClientIpAddress(request),
                request.getHeader("User-Agent")
        ).withRequestId(requestId);

        // 设置到ThreadLocal
        UserContextHolder.setContext(context);

        // 设置响应头
        response.setHeader("X-Request-Id", requestId);

        log.debug("设置用户上下文: userId={}, tenantId={}, requestId={}",
                context.getCurrentUserId(), context.getTenantId(), requestId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理ThreadLocal
        UserContextHolder.clear();
        log.debug("清理用户上下文");
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
