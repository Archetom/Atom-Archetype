#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.rest.interceptor;

import ${package}.domain.context.UserContext;
import ${package}.domain.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * user context
 * @author hanfeng
 */
@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // from request in get user (actual in can from JWT token in)
        String userIdHeader = request.getHeader("X-User-Id");
        String tenantIdHeader = request.getHeader("X-Tenant-Id");
        String adminHeader = request.getHeader("X-Admin");
        String requestId = request.getHeader("X-Request-Id");

        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        // create user context
        UserContext context = UserContext.create(
                userIdHeader != null ? Long.parseLong(userIdHeader) : null,
                tenantIdHeader != null ? Long.parseLong(tenantIdHeader) : null
        ).withOperation(
                request.getMethod() + " " + request.getRequestURI(),
                getClientIpAddress(request),
                request.getHeader("User-Agent")
        ).withRequestId(requestId)
         .withAdmin("true".equalsIgnoreCase(adminHeader));

        // set to ThreadLocal
        UserContextHolder.setContext(context);

        // set response
        response.setHeader("X-Request-Id", requestId);

        log.debug(" set user context: userId={}, tenantId={}, requestId={}",
                context.getCurrentUserId(), context.getTenantId(), requestId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // clean ThreadLocal
        UserContextHolder.clear();
        log.debug(" clean user context ");
    }

    /**
     * get client IP
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
