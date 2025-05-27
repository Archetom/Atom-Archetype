package ${package}.domain.event;

import ${package}.api.enums.UserStatus;
import lombok.Getter;

/**
 * 用户状态变更事件
 * @author hanfeng
 */
@Getter
public class UserStatusChangedEvent extends BaseDomainEvent {

    private final Long userId;
    private final UserStatus oldStatus;
    private final UserStatus newStatus;
    private final String reason;

    public UserStatusChangedEvent(Long userId, UserStatus oldStatus, UserStatus newStatus, String reason) {
        super();
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    @Override
    public String getAggregateId() {
        return userId.toString();
    }
}
