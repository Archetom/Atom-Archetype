#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * request DTO (Data Transfer Object) package.
 * <p>
 * used for define API layer of request parameter data structure, responsible for frontend or client of data.
 * </p>
 * <p>
 * Request DTOs for receiving API parameters.
 * </p>
 * <pre>
 * public class UserQueryRequest {
 *     private Long userId;
 *     private String name;
 * }
 * </pre>
 */
package ${package}.api.dto.request;
