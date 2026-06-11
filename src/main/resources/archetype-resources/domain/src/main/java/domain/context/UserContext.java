#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.context;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * user domain context
 * @author hanfeng
 */
@Data
@Accessors(chain = true)
public class UserContext {

    /**
     * current user ID
     */
    private Long currentUserId;

    /**
     * tenant ID
     */
    private Long tenantId;

    /**
     *
     */
    private LocalDateTime operationTime;

    /**
     * class
     */
    private String operationType;

    /**
     * client IP
     */
    private String clientIp;

    /**
     * user
     */
    private String userAgent;

    /**
     * request ID (used for)
     */
    private String requestId;

    /**
     * whether as administrator
     */
    private boolean admin;

    /**
     * create user context
     */
    public static UserContext create(Long currentUserId, Long tenantId) {
        return new UserContext()
                .setCurrentUserId(currentUserId)
                .setTenantId(tenantId)
                .setOperationTime(LocalDateTime.now());
    }

    /**
     * set
     */
    public UserContext withOperation(String operationType, String clientIp, String userAgent) {
        return this.setOperationType(operationType)
                .setClientIp(clientIp)
                .setUserAgent(userAgent);
    }

    /**
     * set request ID
     */
    public UserContext withRequestId(String requestId) {
        return this.setRequestId(requestId);
    }

    /**
     * set administrator
     */
    public UserContext withAdmin(boolean admin) {
        return this.setAdmin(admin);
    }

    /**
     * check whether permission tenant of data
     */
    public boolean canAccessTenant(Long targetTenantId) {
        return this.tenantId != null && this.tenantId.equals(targetTenantId);
    }

    /**
     * check whether as System Administrator
     */
    public boolean isSystemAdmin() {
        return this.admin;
    }
}
