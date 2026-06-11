package ${package}.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * domain event interface
 * @author hanfeng
 */
public interface DomainEvent {

    /**
     * event ID
     */
    String getEventId();

    /**
     * event class
     */
    String getEventType();

    /**
     *
     */
    LocalDateTime getOccurredOn();

    /**
     * aggregate root ID
     */
    String getAggregateId();

    /**
     * event
     */
    default Integer getVersion() {
        return 1;
    }
}
