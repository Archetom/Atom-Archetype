#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * 应用层事件监听器包。
 * <p>
 * 定义应用层事件的监听器，处理事件驱动的业务逻辑。
 * </p>
 * <p>
 * Application layer event listeners for event-driven business logic.
 * </p>
 * <pre>
 * @EventListener
 * public void handleUserCreated(UserCreatedApplicationEvent event) {
 *     // 处理用户创建事件
 * }
 * </pre>
 */
package ${package}.application.event.listener;