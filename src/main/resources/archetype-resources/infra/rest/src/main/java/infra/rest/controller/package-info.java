#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * REST 控制器包。
 * <p>
 * 定义对外开放的 RESTful API 接口。
 * </p>
 * <p>
 * REST controllers for exposing APIs.
 * </p>
 * <pre>
 * @RestController
 * public class UserController {
 *     @GetMapping("/users/{id}")
 *     public UserResponse getUserById(@PathVariable Long id) { ... }
 * }
 * </pre>
 */
package ${package}.infra.rest.controller;
