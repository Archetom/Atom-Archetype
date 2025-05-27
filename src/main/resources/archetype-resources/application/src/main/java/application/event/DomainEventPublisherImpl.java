package ${package}.application.event;

import ${package}.domain.event.DomainEvent;
import ${package}.domain.event.DomainEventPublisher;
import ${package}.domain.event.UserCreatedEvent;
import ${package}.domain.event.UserStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 领域事件发布器实现
 * @author hanfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(DomainEvent domainEvent) {
        if (domainEvent == null) {
            return;
        }

        log.debug("发布领域事件: {}, 聚合ID: {}", domainEvent.getEventType(), domainEvent.getAggregateId());

        // 直接发布领域事件，让Spring事件监听器处理
        applicationEventPublisher.publishEvent(domainEvent);
    }

    @Override
    public void publishAll(List<DomainEvent> domainEvents) {
        if (domainEvents == null || domainEvents.isEmpty()) {
            return;
        }

        domainEvents.forEach(this::publish);
    }
}
