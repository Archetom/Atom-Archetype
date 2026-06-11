#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * infrastructure messaging package.
 * <p>
 * responsible for integrate with message queue etc. message infrastructure.
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
