#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application.converter;

import ${package}.api.dto.response.UserResponse;
import ${package}.api.enums.UserStatus;
import ${package}.application.vo.UserVO;
import ${package}.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
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
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToCode")
    @Mapping(target = "statusName", source = "status", qualifiedByName = "statusToName")
    @Mapping(target = "active", source = "status", qualifiedByName = "statusToActive")
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

    /**
     * 状态枚举转换为代码
     */
    @Named("statusToCode")
    default String statusToCode(UserStatus status) {
        return status != null ? status.getCode() : null;
    }

    /**
     * 状态枚举转换为描述
     */
    @Named("statusToName")
    default String statusToName(UserStatus status) {
        return status != null ? status.getName() : null;
    }

    /**
     * 状态枚举转换为是否激活
     */
    @Named("statusToActive")
    default Boolean statusToActive(UserStatus status) {
        return status == UserStatus.ACTIVE;
    }
}
