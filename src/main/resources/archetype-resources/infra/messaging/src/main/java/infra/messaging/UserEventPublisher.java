#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.messaging;

import ${package}.domain.event.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * user event publisher
 * @author hanfeng
 */
@Slf4j
@Component
public class UserEventPublisher {

    @EventListener
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info(" process user create event: userId={}, username={}, email={}",
                event.getUserId(), event.getUsername(), event.getEmail());

        // TODO: send to message queue (such as Kafka, RabbitMQ etc.)
        publishToMessageQueue("user.created", event);
    }

    private void publishToMessageQueue(String topic, Object event) {
        // simulate send to message queue
        log.info(" send event to message queue - Topic: {}, Event: {}", topic, event);
        // actual implementation in can integration Spring Cloud Stream, Kafka Template etc.
    }
}
