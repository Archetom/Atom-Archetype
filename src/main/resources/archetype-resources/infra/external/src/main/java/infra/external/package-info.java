#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * Infrastructure External package.
 * <p>
 * Contains implementations for external integrations such as third-party APIs or libraries.
 * </p>
 * <p>Example:</p>
 * <pre>
 * public class PaymentGatewayImpl implements PaymentGateway {
 *     public boolean processPayment(Long userId, Double amount) {
 *         // Integration logic here
 *     }
 * }
 * </pre>
 */
package ${package}.infra.external;
