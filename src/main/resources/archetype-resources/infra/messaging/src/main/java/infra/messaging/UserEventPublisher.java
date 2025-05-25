#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.messaging;

import ${package}.domain.messaging.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户事件发布器
 * @author hanfeng
 */
@Slf4j
@Component
public class UserEventPublisher {

    @EventListener
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info("处理用户创建事件: userId={}, username={}, email={}",
                event.getUserId(), event.getUsername(), event.getEmail());

        // TODO: 发送到消息队列（如 Kafka、RabbitMQ 等）
        publishToMessageQueue("user.created", event);
    }

    private void publishToMessageQueue(String topic, Object event) {
        // 模拟发送到消息队列
        log.info("发送事件到消息队列 - Topic: {}, Event: {}", topic, event);
        // 实际实现中可以集成 Spring Cloud Stream、Kafka Template 等
    }
}
