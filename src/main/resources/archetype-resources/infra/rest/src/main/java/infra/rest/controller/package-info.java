#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * REST controller package.
 * <p>
 * define for of RESTful API interface.
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
