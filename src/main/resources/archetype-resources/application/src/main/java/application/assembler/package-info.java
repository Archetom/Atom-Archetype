#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * 装配器（Assembler）包。
 * <p>
 * 负责在不同层（如 domain、dto、vo）间的数据对象转换。
 * </p>
 * <p>
 * Assemblers for mapping between domain, DTO, VO, etc.
 * </p>
 * <pre>
 * public class UserAssembler {
 *     public static UserDTO toDTO(User user) {
 *         // Mapping logic
 *     }
 * }
 * </pre>
 */
package ${package}.application.assembler;
