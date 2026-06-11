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
 * user controller
 * @author hanfeng
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserFacade userFacade;
    
    /**
     * create user
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody @Validated UserCreateRequest request) {
        log.info(" create user request: {}", request);
        Result<UserResponse> result = userFacade.createUser(request);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    /**
     * based on ID get user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        log.info(" get user request, userId: {}", userId);
        Result<UserResponse> result = userFacade.getUserById(userId);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    /**
     * paged query user
     */
    @GetMapping
    public ResponseEntity<?> queryUsers(UserQueryRequest request) {
        log.info(" paged query user request: {}", request);
        Result<Pager<UserResponse>> result = userFacade.queryUsers(request);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    /**
     * update user status
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
        @PathVariable Long userId,
        @RequestParam String status
    ) {
        log.info(" update user status request, userId: {}, status: {}", userId, status);
        Result<Void> result = userFacade.updateUserStatus(userId, status);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    /**
     * delete user
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        log.info(" delete user request, userId: {}", userId);
        Result<Void> result = userFacade.deleteUser(userId);
        return ResponseEntityUtil.assembleResponse(result);
    }
}
