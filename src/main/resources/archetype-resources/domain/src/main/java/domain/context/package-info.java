#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * 领域上下文包。
 * <p>
 * 用于定义领域模型操作时的上下文环境和边界。
 * </p>
 * <p>
 * Domain context for aggregating context data in business logic.
 * </p>
 * <pre>
 * public class UserContext {
 *     private Long userId;
 *     private String role;
 * }
 * </pre>
 */
package ${package}.domain.context;
