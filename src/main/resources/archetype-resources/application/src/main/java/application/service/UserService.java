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
 * user application service interface
 * @author hanfeng
 */
public interface UserService {
    
    /**
     * create user
     */
    Result<UserVO> createUser(UserCreateRequest request);
    
    /**
     * based on ID get user
     */
    Result<UserVO> getUserById(Long userId);
    
    /**
     * paged query user
     */
    Result<Pager<UserVO>> queryUsers(UserQueryRequest request);
    
    /**
     * update user status
     */
    Result<Void> updateUserStatus(Long userId, String status);
    
    /**
     * delete user
     */
    Result<Void> deleteUser(Long userId);
}
