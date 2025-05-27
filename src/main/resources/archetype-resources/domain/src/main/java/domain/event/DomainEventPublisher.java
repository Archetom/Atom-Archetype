package ${package}.domain.event;

import java.util.List;

/**
 * 领域事件发布器接口 - 在领域层定义，应用层实现
 * @author hanfeng
 */
public interface DomainEventPublisher {

    /**
     * 发布单个领域事件
     */
    void publish(DomainEvent domainEvent);

    /**
     * 批量发布领域事件
     */
    void publishAll(List<DomainEvent> domainEvents);
}
