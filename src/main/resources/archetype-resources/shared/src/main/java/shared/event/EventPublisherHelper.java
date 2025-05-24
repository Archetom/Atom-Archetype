package ${package}.shared.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 事件发布工具
 */
@Component
public class EventPublisherHelper {
    private final ApplicationEventPublisher publisher;

    public EventPublisherHelper(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(ApplicationEvent event) {
        publisher.publishEvent(event);
    }
}