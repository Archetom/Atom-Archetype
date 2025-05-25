#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.context;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户领域上下文
 * @author hanfeng
 */
@Data
@Accessors(chain = true)
public class UserContext {

    /**
     * 当前操作用户ID
     */
    private Long currentUserId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 请求ID（用于链路追踪）
     */
    private String requestId;

    /**
     * 创建用户上下文
     */
    public static UserContext create(Long currentUserId, Long tenantId) {
        return new UserContext()
                .setCurrentUserId(currentUserId)
                .setTenantId(tenantId)
                .setOperationTime(LocalDateTime.now());
    }

    /**
     * 设置操作信息
     */
    public UserContext withOperation(String operationType, String clientIp, String userAgent) {
        return this.setOperationType(operationType)
                .setClientIp(clientIp)
                .setUserAgent(userAgent);
    }

    /**
     * 设置请求ID
     */
    public UserContext withRequestId(String requestId) {
        return this.setRequestId(requestId);
    }

    /**
     * 检查是否有权限操作指定租户的数据
     */
    public boolean canAccessTenant(Long targetTenantId) {
        return this.tenantId != null && this.tenantId.equals(targetTenantId);
    }

    /**
     * 检查是否为系统管理员
     */
    public boolean isSystemAdmin() {
        return this.currentUserId != null && this.currentUserId.equals(1L);
    }
}
