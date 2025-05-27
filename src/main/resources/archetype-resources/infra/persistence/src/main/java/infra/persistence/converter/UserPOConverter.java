package ${package}.infra.persistence.converter;

import ${package}.api.enums.UserStatus;
import ${package}.domain.entity.User;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import ${package}.infra.persistence.mysql.po.UserPO;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
    @Mapping(target = "id", expression = "java(longToUserId(userPO.getId()))")
    @Mapping(target = "username", expression = "java(stringToUsername(userPO.getUsername()))")
    @Mapping(target = "email", expression = "java(stringToEmail(userPO.getEmail()))")
    @Mapping(target = "phoneNumber", expression = "java(stringToPhoneNumber(userPO.getPhoneNumber()))")
    @Mapping(target = "status", expression = "java(codeToStatus(userPO.getStatus()))")
    @Mapping(target = "externalUser", source = "externalUser")
    @Mapping(target = "admin", source = "admin")
    @Mapping(target = "externalId", source = "externalId")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "realName", source = "realName")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createdTime", source = "createdTime")
    @Mapping(target = "updatedTime", source = "updatedTime")
    User toDomain(UserPO userPO);

    /**
     * User -> UserPO
     */
    @Mapping(target = "id", expression = "java(userIdToLong(user.getId()))")
    @Mapping(target = "username", expression = "java(usernameToString(user.getUsername()))")
    @Mapping(target = "email", expression = "java(emailToString(user.getEmail()))")
    @Mapping(target = "phoneNumber", expression = "java(phoneNumberToString(user.getPhoneNumber()))")
    @Mapping(target = "status", expression = "java(statusToCode(user.getStatus()))")
    @Mapping(target = "externalUser", source = "externalUser")
    @Mapping(target = "admin", source = "admin")
    @Mapping(target = "externalId", source = "externalId")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "realName", source = "realName")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createdTime", source = "createdTime")
    @Mapping(target = "updatedTime", source = "updatedTime")
    @Mapping(target = "deletedTime", ignore = true)
    UserPO toPO(User user);

    /**
     * UserPO List -> User List
     */
    List<User> toDomainList(List<UserPO> userPOs);

    /**
     * User List -> UserPO List
     */
    List<UserPO> toPOList(List<User> users);

    // ========== 转换方法 ==========

    /**
     * Long -> UserId
     */
    default UserId longToUserId(Long id) {
        return id != null ? new UserId(id) : null;
    }

    /**
     * UserId -> Long
     */
    default Long userIdToLong(UserId userId) {
        return userId != null ? userId.getValue() : null;
    }

    /**
     * String -> Username
     */
    default Username stringToUsername(String username) {
        return StringUtils.isNotBlank(username) ? new Username(username) : null;
    }

    /**
     * Username -> String
     */
    default String usernameToString(Username username) {
        return username != null ? username.getValue() : null;
    }

    /**
     * String -> Email
     */
    default Email stringToEmail(String email) {
        return StringUtils.isNotBlank(email) ? new Email(email) : null;
    }

    /**
     * Email -> String
     */
    default String emailToString(Email email) {
        return email != null ? email.getValue() : null;
    }

    /**
     * String -> PhoneNumber
     */
    default PhoneNumber stringToPhoneNumber(String phoneNumber) {
        return StringUtils.isNotBlank(phoneNumber) ? new PhoneNumber(phoneNumber) : null;
    }

    /**
     * PhoneNumber -> String
     */
    default String phoneNumberToString(PhoneNumber phoneNumber) {
        return phoneNumber != null ? phoneNumber.getValue() : null;
    }

    /**
     * String -> UserStatus
     */
    default UserStatus codeToStatus(String code) {
        return code != null ? UserStatus.fromCode(code) : null;
    }

    /**
     * UserStatus -> String
     */
    default String statusToCode(UserStatus status) {
        return status != null ? status.getCode() : null;
    }
}
