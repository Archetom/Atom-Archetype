#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * 基础设施消息包。
 * <p>
 * 负责对接消息队列等消息基础设施。
 * </p>
 * <p>
 * Messaging infrastructure integration (Kafka, RabbitMQ, etc).
 * </p>
 * <pre>
 * public class UserEventProducer {
 *     public void sendEvent(UserEvent event) { ... }
 * }
 * </pre>
 */
package ${package}.infra.messaging;
