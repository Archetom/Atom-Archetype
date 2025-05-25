#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.converter;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.infra.persistence.mysql.po.UserPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * 用户PO转换器
 * @author hanfeng
 */
@Mapper(componentModel = "spring")
public interface UserPOConverter {

    /**
     * UserPO -> User
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "codeToStatus")
    User toDomain(UserPO userPO);

    /**
     * User -> UserPO
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToCode")
    UserPO toPO(User user);

    /**
     * UserPO List -> User List
     */
    List<User> toDomainList(List<UserPO> userPOs);

    /**
     * User List -> UserPO List
     */
    List<UserPO> toPOList(List<User> users);

    /**
     * 代码转换为状态枚举
     */
    @Named("codeToStatus")
    default UserStatus codeToStatus(String code) {
        return code != null ? UserStatus.fromCode(code) : null;
    }

    /**
     * 状态枚举转换为代码
     */
    @Named("statusToCode")
    default String statusToCode(UserStatus status) {
        return status != null ? status.getCode() : null;
    }
}
