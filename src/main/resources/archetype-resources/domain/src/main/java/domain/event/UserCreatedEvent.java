package ${package}.domain.event;

import lombok.Getter;

/**
 * user create event
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
