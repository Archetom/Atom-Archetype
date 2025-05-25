#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infra.rest.controller;

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserResponse;
import ${package}.api.facade.UserFacade;
import ${package}.infra.rest.util.ResponseEntityUtil;
import io.github.archetom.common.result.Pager;
import io.github.archetom.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * @author hanfeng
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserFacade userFacade;
    
    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody @Validated UserCreateRequest request) {
        log.info("创建用户请求: {}", request);
        Result<UserResponse> result = userFacade.createUser(request);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        log.info("获取用户请求, userId: {}", userId);
        Result<UserResponse> result = userFacade.getUserById(userId);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    /**
     * 分页查询用户
     */
    @GetMapping
    public ResponseEntity<?> queryUsers(UserQueryRequest request) {
        log.info("分页查询用户请求: {}", request);
        Result<Pager<UserResponse>> result = userFacade.queryUsers(request);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
        @PathVariable Long userId,
        @RequestParam String status
    ) {
        log.info("更新用户状态请求, userId: {}, status: {}", userId, status);
        Result<Void> result = userFacade.updateUserStatus(userId, status);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        log.info("删除用户请求, userId: {}", userId);
        Result<Void> result = userFacade.deleteUser(userId);
        return ResponseEntityUtil.assembleResponse(result);
    }
}
