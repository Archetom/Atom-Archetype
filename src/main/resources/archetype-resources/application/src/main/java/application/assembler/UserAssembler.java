package ${package}.application.assembler;

import ${package}.api.dto.response.UserResponse;
import ${package}.application.converter.UserConverter;
import ${package}.application.vo.UserVO;
import ${package}.domain.entity.User;
import ${package}.shared.util.PageUtil;
import io.github.archetom.common.result.Pager;

import java.util.List;

/**
 * 用户装配器
 * @author hanfeng
 */
public class UserAssembler {

    /**
     * Domain User -> UserVO
     */
    public static UserVO toVO(User user) {
        return UserConverter.INSTANCE.toVO(user);
    }

    /**
     * UserVO -> UserResponse
     */
    public static UserResponse toResponse(UserVO userVO) {
        return UserConverter.INSTANCE.toResponse(userVO);
    }

    /**
     * Domain User -> UserResponse
     */
    public static UserResponse toResponse(User user) {
        return toResponse(toVO(user));
    }

    /**
     * Domain User List -> UserVO List
     */
    public static List<UserVO> toVOList(List<User> users) {
        return UserConverter.INSTANCE.toVOList(users);
    }

    /**
     * UserVO List -> UserResponse List
     */
    public static List<UserResponse> toResponseList(List<UserVO> userVOs) {
        return UserConverter.INSTANCE.toResponseList(userVOs);
    }

    /**
     * Domain User Pager -> UserVO Pager
     */
    public static Pager<UserVO> toVOPager(Pager<User> userPager) {
        if (userPager == null) {
            return null;
        }

        Pager<UserVO> voPager = PageUtil.copy(userPager);
        voPager.setObjectList(toVOList(userPager.getObjectList()));
        return voPager;
    }

    /**
     * UserVO Pager -> UserResponse Pager
     */
    public static Pager<UserResponse> toResponsePager(Pager<UserVO> userVOPager) {
        if (userVOPager == null) {
            return null;
        }

        Pager<UserResponse> responsePager = PageUtil.copy(userVOPager);
        responsePager.setObjectList(toResponseList(userVOPager.getObjectList()));
        return responsePager;
    }
}
