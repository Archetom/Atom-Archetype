#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * 领域仓储接口包。
 * <p>
 * 用于定义领域对象的持久化接口。
 * </p>
 * <p>
 * Repository interfaces for persisting domain objects.
 * </p>
 * <pre>
 * public interface UserRepository {
 *     User findById(Long id);
 * }
 * </pre>
 */
package ${package}.domain.repository;