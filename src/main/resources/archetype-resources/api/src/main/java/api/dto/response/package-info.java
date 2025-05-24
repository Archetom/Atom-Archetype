#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * 响应 DTO（Data Transfer Object）包。
 * <p>
 * 用于定义 API 层的响应参数数据结构，负责返回给前端或客户端的数据。
 * </p>
 * <p>
 * Response DTOs for sending API results.
 * </p>
 * <pre>
 * public class UserResponse {
 *     private Long userId;
 *     private String username;
 * }
 * </pre>
 */
package ${package}.api.dto.response;
