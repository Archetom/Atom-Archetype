#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * application layer event package.
 * <p>
 * define application layer event of, process event of business logic.
 * </p>
 * <p>
 * Application layer event listeners for event-driven business logic.
 * </p>
 * <pre>
 * @EventListener
 * public void handleUserCreated(UserCreatedApplicationEvent event) {
 * // process user create event
 * }
 * </pre>
 */
package ${package}.application.event.listener;