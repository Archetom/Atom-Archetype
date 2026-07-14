#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.facade;

import ${package}.api.context.AuthenticatedCaller;
import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserResponse;
import ${package}.api.facade.UserFacade;
import ${package}.application.assembler.UserAssembler;
import ${package}.application.service.UserService;
import ${package}.application.vo.UserVO;
import ${package}.shared.util.ResultUtil;
import io.github.archetom.common.result.Pager;
import io.github.archetom.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * user facade implementation
 * @author hanfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final UserService userService;

    @Override
    public Result<UserResponse> createUser(AuthenticatedCaller caller, UserCreateRequest request) {
        return ResultUtil.map(userService.createUser(caller, request), UserAssembler.INSTANCE::toResponse);
    }

    @Override
    public Result<UserResponse> getUserById(AuthenticatedCaller caller, Long userId) {
        return ResultUtil.map(userService.getUserById(caller, userId), UserAssembler.INSTANCE::toResponse);
    }

    @Override
    public Result<Pager<UserResponse>> queryUsers(AuthenticatedCaller caller, UserQueryRequest request) {
        return ResultUtil.map(userService.queryUsers(caller, request), UserAssembler.INSTANCE::toResponsePager);
    }

    @Override
    public Result<Void> updateUserStatus(AuthenticatedCaller caller, Long userId, String status) {
        return userService.updateUserStatus(caller, userId, status);
    }

    @Override
    public Result<Void> deleteUser(AuthenticatedCaller caller, Long userId) {
        return userService.deleteUser(caller, userId);
    }
}
