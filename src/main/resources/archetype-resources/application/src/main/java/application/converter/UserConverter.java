package ${package}.application.converter;

import ${package}.api.dto.response.UserResponse;
import ${package}.application.vo.UserVO;
import ${package}.domain.entity.User;
import ${package}.domain.model.UserStatus;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.TenantId;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/** MapStruct mappings for User aggregate, application VO, and public response. */
@Mapper
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * Domain User -> UserVO
     */
    @Mapping(target = "id", expression = "java(userIdToLong(user.getId()))")
    @Mapping(target = "username", expression = "java(usernameToString(user.getUsername()))")
    @Mapping(target = "email", expression = "java(emailToString(user.getEmail()))")
    @Mapping(target = "maskedPhoneNumber", expression = "java(phoneNumberToMasked(user.getPhoneNumber()))")
    @Mapping(target = "status", expression = "java(statusToCode(user.getStatus()))")
    @Mapping(target = "statusName", expression = "java(statusToName(user.getStatus()))")
    @Mapping(target = "active", expression = "java(statusToActive(user.getStatus()))")
    @Mapping(target = "tenantId", expression = "java(tenantIdToLong(user.getTenantId()))")
    UserVO toVO(User user);

    /**
     * UserVO -> UserResponse
     */
    UserResponse toResponse(UserVO userVO);

    /**
     * Domain User List -> UserVO List
     */
    List<UserVO> toVOList(List<User> users);

    /**
     * UserVO List -> UserResponse List
     */
    List<UserResponse> toResponseList(List<UserVO> userVOs);

    // ========== Value conversions ==========

    /**
     * UserId -> Long
     */
    default Long userIdToLong(UserId userId) {
        return userId != null ? userId.getValue() : null;
    }

    default Long tenantIdToLong(TenantId tenantId) {
        return tenantId != null ? tenantId.getValue() : null;
    }

    /**
     * Username -> String
     */
    default String usernameToString(Username username) {
        return username != null ? username.getValue() : null;
    }

    /**
     * Email -> String
     */
    default String emailToString(Email email) {
        return email != null ? email.getValue() : null;
    }

    /**
     * PhoneNumber -> masked string
     */
    default String phoneNumberToMasked(PhoneNumber phoneNumber) {
        return phoneNumber != null ? phoneNumber.getMasked() : null;
    }

    /**
     * status convert as code
     */
    default String statusToCode(UserStatus status) {
        return status != null ? status.getCode() : null;
    }

    /**
     * status convert as
     */
    default String statusToName(UserStatus status) {
        return status != null ? status.getDisplayName() : null;
    }

    /**
     * status convert as whether active
     */
    default Boolean statusToActive(UserStatus status) {
        return status == UserStatus.ACTIVE;
    }
}
