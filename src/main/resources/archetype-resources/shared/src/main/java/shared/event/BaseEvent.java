package ${package}.shared.event;

import org.springframework.context.ApplicationEvent;

/**
 * 基础应用事件，可被 Spring 事件发布监听
 */
public class BaseEvent extends ApplicationEvent {
    public BaseEvent(Object source) {
        super(source);
    }
}
