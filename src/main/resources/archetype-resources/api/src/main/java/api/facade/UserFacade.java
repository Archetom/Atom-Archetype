#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.facade;

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserResponse;
import io.github.archetom.common.result.Pager;
import io.github.archetom.common.result.Result;

/**
 * 用户门面接口
 * @author hanfeng
 */
public interface UserFacade {
    
    /**
     * 创建用户
     */
    Result<UserResponse> createUser(UserCreateRequest request);
    
    /**
     * 根据ID获取用户
     */
    Result<UserResponse> getUserById(Long userId);
    
    /**
     * 分页查询用户
     */
    Result<Pager<UserResponse>> queryUsers(UserQueryRequest request);
    
    /**
     * 更新用户状态
     */
    Result<Void> updateUserStatus(Long userId, String status);
    
    /**
     * 删除用户
     */
    Result<Void> deleteUser(Long userId);
}
