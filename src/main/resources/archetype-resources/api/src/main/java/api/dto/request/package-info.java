#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * 请求 DTO（Data Transfer Object）包。
 * <p>
 * 用于定义 API 层的请求参数数据结构，负责接收前端或客户端传入的数据。
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
