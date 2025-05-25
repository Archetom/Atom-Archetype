#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.application.service;

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.application.vo.UserVO;
import io.github.archetom.common.result.Pager;
import io.github.archetom.common.result.Result;

/**
 * 用户应用服务接口
 * @author hanfeng
 */
public interface UserService {
    
    /**
     * 创建用户
     */
    Result<UserVO> createUser(UserCreateRequest request);
    
    /**
     * 根据ID获取用户
     */
    Result<UserVO> getUserById(Long userId);
    
    /**
     * 分页查询用户
     */
    Result<Pager<UserVO>> queryUsers(UserQueryRequest request);
    
    /**
     * 更新用户状态
     */
    Result<Void> updateUserStatus(Long userId, String status);
    
    /**
     * 删除用户
     */
    Result<Void> deleteUser(Long userId);
}
