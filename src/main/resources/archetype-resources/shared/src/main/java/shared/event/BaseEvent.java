package ${package}.shared.event;

import org.springframework.context.ApplicationEvent;

/**
 * base application event, can Spring event publish
 */
public class BaseEvent extends ApplicationEvent {
    public BaseEvent(Object source) {
        super(source);
    }
}
