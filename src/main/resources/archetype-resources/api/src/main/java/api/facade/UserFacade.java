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
 * user facade interface
 * @author hanfeng
 */
public interface UserFacade {
    
    /**
     * create user
     */
    Result<UserResponse> createUser(UserCreateRequest request);
    
    /**
     * based on ID get user
     */
    Result<UserResponse> getUserById(Long userId);
    
    /**
     * paged query user
     */
    Result<Pager<UserResponse>> queryUsers(UserQueryRequest request);
    
    /**
     * update user status
     */
    Result<Void> updateUserStatus(Long userId, String status);
    
    /**
     * delete user
     */
    Result<Void> deleteUser(Long userId);
}
