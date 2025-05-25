#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.persistence.converter;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.infra.persistence.mysql.po.UserPO;
import ${package}.shared.types.Email;
import ${package}.shared.types.PhoneNumber;
import org.apache.commons.lang3.StringUtils;
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
    @Mapping(target = "email", source = "email", qualifiedByName = "stringToEmail")
    @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "stringToPhoneNumber")
    User toDomain(UserPO userPO);

    /**
     * User -> UserPO
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToCode")
    @Mapping(target = "email", source = "email", qualifiedByName = "emailToString")
    @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "phoneNumberToString")
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

    /**
     * 字符串转Email值对象
     */
    @Named("stringToEmail")
    default Email stringToEmail(String email) {
        return StringUtils.isNotBlank(email) ? new Email(email) : null;
    }

    /**
     * Email值对象转字符串
     */
    @Named("emailToString")
    default String emailToString(Email email) {
        return email != null ? email.getValue() : null;
    }

    /**
     * 字符串转PhoneNumber值对象
     */
    @Named("stringToPhoneNumber")
    default PhoneNumber stringToPhoneNumber(String phoneNumber) {
        return StringUtils.isNotBlank(phoneNumber) ? new PhoneNumber(phoneNumber) : null;
    }

    /**
     * PhoneNumber值对象转字符串
     */
    @Named("phoneNumberToString")
    default String phoneNumberToString(PhoneNumber phoneNumber) {
        return phoneNumber != null ? phoneNumber.getValue() : null;
    }
}
