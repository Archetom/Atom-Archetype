#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * The Convertor package.
 * <p>
 * Contains classes responsible for simple type and field mapping between entities, DTOs, and VOs.
 * <p>
 * Typically, convertors handle one-to-one property conversions and are often implemented using MapStruct or manual mapping methods.
 * </p>
 * <p>Example:</p>
 * <pre>
 * public class UserConvertor {
 *     public static UserDTO toDTO(User user) {
 *         // Mapping logic
 *     }
 * }
 * </pre>
 */
package ${package}.application.convertor;