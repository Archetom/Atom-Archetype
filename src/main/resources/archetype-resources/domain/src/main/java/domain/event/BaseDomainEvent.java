package ${package}.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件基类
 * @author hanfeng
 */
@Getter
public abstract class BaseDomainEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
