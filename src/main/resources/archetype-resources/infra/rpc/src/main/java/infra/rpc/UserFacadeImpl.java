#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.rpc;

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserResponse;
import ${package}.api.facade.UserFacade;
import ${package}.application.assembler.UserAssembler;
import ${package}.application.service.UserService;
import ${package}.application.vo.UserVO;
import io.github.archetom.common.result.Pager;
import io.github.archetom.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户门面实现
 * @author hanfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {
    
    private final UserService userService;
    
    @Override
    public Result<UserResponse> createUser(UserCreateRequest request) {
        Result<UserVO> result = userService.createUser(request);
        
        if (!result.isSuccess()) {
            Result<UserResponse> errorResult = new Result<>();
            errorResult.setSuccess(false);
            errorResult.setErrorContext(result.getErrorContext());
            return errorResult;
        }
        
        UserResponse response = UserAssembler.toResponse(result.getData());
        
        Result<UserResponse> successResult = new Result<>();
        successResult.setSuccess(true);
        successResult.setData(response);
        return successResult;
    }
    
    @Override
    public Result<UserResponse> getUserById(Long userId) {
        Result<UserVO> result = userService.getUserById(userId);
        
        if (!result.isSuccess()) {
            Result<UserResponse> errorResult = new Result<>();
            errorResult.setSuccess(false);
            errorResult.setErrorContext(result.getErrorContext());
            return errorResult;
        }
        
        UserResponse response = UserAssembler.toResponse(result.getData());
        
        Result<UserResponse> successResult = new Result<>();
        successResult.setSuccess(true);
        successResult.setData(response);
        return successResult;
    }
    
    @Override
    public Result<Pager<UserResponse>> queryUsers(UserQueryRequest request) {
        Result<Pager<UserVO>> result = userService.queryUsers(request);
        
        if (!result.isSuccess()) {
            Result<Pager<UserResponse>> errorResult = new Result<>();
            errorResult.setSuccess(false);
            errorResult.setErrorContext(result.getErrorContext());
            return errorResult;
        }
        
        Pager<UserResponse> responsePager = UserAssembler.toResponsePager(result.getData());
        
        Result<Pager<UserResponse>> successResult = new Result<>();
        successResult.setSuccess(true);
        successResult.setData(responsePager);
        return successResult;
    }
    
    @Override
    public Result<Void> updateUserStatus(Long userId, String status) {
        return userService.updateUserStatus(userId, status);
    }
    
    @Override
    public Result<Void> deleteUser(Long userId) {
        return userService.deleteUser(userId);
    }
}
