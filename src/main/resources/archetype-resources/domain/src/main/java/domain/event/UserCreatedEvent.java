package ${package}.domain.event;

import lombok.Getter;

/**
 * 用户创建事件
 * @author hanfeng
 */
@Getter
public class UserCreatedEvent extends BaseDomainEvent {

    private final Long userId;
    private final String username;
    private final String email;

    public UserCreatedEvent(Long userId, String username, String email) {
        super();
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    @Override
    public String getAggregateId() {
        return userId.toString();
    }
}
