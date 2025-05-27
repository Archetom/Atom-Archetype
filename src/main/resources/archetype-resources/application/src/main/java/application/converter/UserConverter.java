package ${package}.application.converter;

import ${package}.api.dto.response.UserResponse;
import ${package}.api.enums.UserStatus;
import ${package}.application.vo.UserVO;
import ${package}.domain.entity.User;
import ${package}.domain.valueobject.Email;
import ${package}.domain.valueobject.PhoneNumber;
import ${package}.domain.valueobject.UserId;
import ${package}.domain.valueobject.Username;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户转换器
 * @author hanfeng
 */
@Mapper
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * Domain User -> UserVO
     */
    @Mapping(target = "id", expression = "java(userIdToLong(user.getId()))")
    @Mapping(target = "username", expression = "java(usernameToString(user.getUsername()))")
    @Mapping(target = "email", expression = "java(emailToString(user.getEmail()))")
    @Mapping(target = "phoneNumber", expression = "java(phoneNumberToString(user.getPhoneNumber()))")
    @Mapping(target = "maskedPhoneNumber", expression = "java(phoneNumberToMasked(user.getPhoneNumber()))")
    @Mapping(target = "status", expression = "java(statusToCode(user.getStatus()))")
    @Mapping(target = "statusName", expression = "java(statusToName(user.getStatus()))")
    @Mapping(target = "active", expression = "java(statusToActive(user.getStatus()))")
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

    // ========== 转换方法 ==========

    /**
     * UserId -> Long
     */
    default Long userIdToLong(UserId userId) {
        return userId != null ? userId.getValue() : null;
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
     * PhoneNumber -> String
     */
    default String phoneNumberToString(PhoneNumber phoneNumber) {
        return phoneNumber != null ? phoneNumber.getValue() : null;
    }

    /**
     * PhoneNumber -> 脱敏字符串
     */
    default String phoneNumberToMasked(PhoneNumber phoneNumber) {
        return phoneNumber != null ? phoneNumber.getMasked() : null;
    }

    /**
     * 状态枚举转换为代码
     */
    default String statusToCode(UserStatus status) {
        return status != null ? status.getCode() : null;
    }

    /**
     * 状态枚举转换为描述
     */
    default String statusToName(UserStatus status) {
        return status != null ? status.getName() : null;
    }

    /**
     * 状态枚举转换为是否激活
     */
    default Boolean statusToActive(UserStatus status) {
        return status == UserStatus.ACTIVE;
    }
}
