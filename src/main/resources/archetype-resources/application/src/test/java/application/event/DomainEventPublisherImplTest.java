package ${package}.application.event;

import ${package}.domain.event.BaseDomainEvent;
import ${package}.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

/** The publisher is generic infrastructure and must survive removal of the User sample. */
@ExtendWith(MockitoExtension.class)
class DomainEventPublisherImplTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void publishesEventsInOrderThroughSpring() {
        DomainEventPublisherImpl publisher = new DomainEventPublisherImpl(applicationEventPublisher);
        DomainEvent first = new TestEvent("1");
        DomainEvent second = new TestEvent("2");

        publisher.publishAll(List.of(first, second));

        InOrder order = inOrder(applicationEventPublisher);
        order.verify(applicationEventPublisher).publishEvent(first);
        order.verify(applicationEventPublisher).publishEvent(second);
    }

    @Test
    void ignoresMissingEvents() {
        DomainEventPublisherImpl publisher = new DomainEventPublisherImpl(applicationEventPublisher);

        publisher.publish(null);
        publisher.publishAll(null);
        publisher.publishAll(List.of());

        verifyNoInteractions(applicationEventPublisher);
    }

    private static final class TestEvent extends BaseDomainEvent {

        private final String aggregateId;

        private TestEvent(String aggregateId) {
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }
}
