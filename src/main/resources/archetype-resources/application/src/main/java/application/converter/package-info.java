/**
 * 类型转换器（Converter）包。
 * <p>
 * 用于实现简单的属性映射和类型转换（常配合 MapStruct 或手工转换）。
 * </p>
 * <p>
 * Converters for mapping between entities, DTOs, and VOs.
 * </p>
 * <pre>
 * public class UserConverter {
 *     public static UserDTO toDTO(User user) {
 *         // Mapping logic
 *     }
 * }
 * </pre>
 */
package ${package}.application.converter;