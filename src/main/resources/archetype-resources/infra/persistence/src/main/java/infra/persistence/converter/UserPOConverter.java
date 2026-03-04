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
import java.util.stream.Collectors;

/**
 * 用户PO转换器
 * @author hanfeng
 */
@Mapper(componentModel = "spring")
public abstract class UserPOConverter {

    /**
     * UserPO -> User（使用 reconstitute 方法重建领域对象）
     */
    public User toDomain(UserPO userPO) {
        if (userPO == null) {
            return null;
        }

        return User.reconstitute(
                longToUserId(userPO.getId()),
                stringToUsername(userPO.getUsername()),
                stringToEmail(userPO.getEmail()),
                stringToPhoneNumber(userPO.getPhoneNumber()),
                userPO.getPassword(),
                userPO.getRealName(),
                codeToStatus(userPO.getStatus()),
                userPO.getTenantId(),
                userPO.getExternalId(),
                Boolean.TRUE.equals(userPO.getExternalUser()),
                Boolean.TRUE.equals(userPO.getAdmin()),
                userPO.getCreatedTime(),
                userPO.getUpdatedTime()
        );
    }

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
    public abstract UserPO toPO(User user);

    /**
     * UserPO List -> User List
     */
    public List<User> toDomainList(List<UserPO> userPOs) {
        if (userPOs == null) {
            return null;
        }
        return userPOs.stream().map(this::toDomain).collect(Collectors.toList());
    }

    /**
     * User List -> UserPO List
     */
    public abstract List<UserPO> toPOList(List<User> users);

    // ========== 转换方法 ==========

    /**
     * Long -> UserId
     */
    protected UserId longToUserId(Long id) {
        return id != null ? new UserId(id) : null;
    }

    /**
     * UserId -> Long
     */
    protected Long userIdToLong(UserId userId) {
        return userId != null ? userId.getValue() : null;
    }

    /**
     * String -> Username
     */
    protected Username stringToUsername(String username) {
        return StringUtils.isNotBlank(username) ? new Username(username) : null;
    }

    /**
     * Username -> String
     */
    protected String usernameToString(Username username) {
        return username != null ? username.getValue() : null;
    }

    /**
     * String -> Email
     */
    protected Email stringToEmail(String email) {
        return StringUtils.isNotBlank(email) ? new Email(email) : null;
    }

    /**
     * Email -> String
     */
    protected String emailToString(Email email) {
        return email != null ? email.getValue() : null;
    }

    /**
     * String -> PhoneNumber
     */
    protected PhoneNumber stringToPhoneNumber(String phoneNumber) {
        return StringUtils.isNotBlank(phoneNumber) ? new PhoneNumber(phoneNumber) : null;
    }

    /**
     * PhoneNumber -> String
     */
    protected String phoneNumberToString(PhoneNumber phoneNumber) {
        return phoneNumber != null ? phoneNumber.getValue() : null;
    }

    /**
     * String -> UserStatus
     */
    protected UserStatus codeToStatus(String code) {
        return code != null ? UserStatus.fromCode(code) : null;
    }

    /**
     * UserStatus -> String
     */
    protected String statusToCode(UserStatus status) {
        return status != null ? status.getCode() : null;
    }
}
