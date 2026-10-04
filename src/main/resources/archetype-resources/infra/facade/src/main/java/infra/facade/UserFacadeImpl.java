#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.facade;

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserPageResponse;
import ${package}.api.dto.response.UserResponse;
import ${package}.api.facade.UserFacade;
import ${package}.application.assembler.UserAssembler;
import ${package}.application.security.AuthenticatedCaller;
import ${package}.application.service.UserService;
import ${package}.infra.security.AuthenticatedCallerResolver;
import ${package}.shared.util.ResultUtil;
import io.github.archetom.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * User facade served to HTTP and RPC clients.
 *
 * <p>The caller always comes from the transport's verified authentication, never from method
 * arguments. A call without a verified caller reaches the use case as {@code null} and is
 * rejected with {@code AUTHENTICATION_REQUIRED}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final UserService userService;
    private final AuthenticatedCallerResolver callerResolver;

    @Override
    public Result<UserResponse> createUser(UserCreateRequest request) {
        return ResultUtil.map(userService.createUser(currentCaller(), request), UserAssembler.INSTANCE::toResponse);
    }

    @Override
    public Result<UserResponse> getUserById(Long userId) {
        return ResultUtil.map(userService.getUserById(currentCaller(), userId), UserAssembler.INSTANCE::toResponse);
    }

    @Override
    public Result<UserPageResponse> queryUsers(UserQueryRequest request) {
        return ResultUtil.map(userService.queryUsers(currentCaller(), request), UserAssembler.INSTANCE::toPageResponse);
    }

    @Override
    public Result<Void> updateUserStatus(Long userId, String status) {
        return userService.updateUserStatus(currentCaller(), userId, status);
    }

    @Override
    public Result<Void> deleteUser(Long userId) {
        return userService.deleteUser(currentCaller(), userId);
    }

    private AuthenticatedCaller currentCaller() {
        return callerResolver.currentCaller().orElse(null);
    }
}
