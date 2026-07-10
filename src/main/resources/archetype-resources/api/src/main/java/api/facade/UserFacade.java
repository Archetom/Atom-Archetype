#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.facade;

import ${package}.api.context.AuthenticatedCaller;
import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserResponse;
import io.github.archetom.common.result.Pager;
import io.github.archetom.common.result.Result;

/** Public facade contract for the bundled User example. */
public interface UserFacade {
    
    /** Create a user inside the caller's tenant. */
    Result<UserResponse> createUser(AuthenticatedCaller caller, UserCreateRequest request);
    
    /** Get a visible user by tenant-scoped ID. */
    Result<UserResponse> getUserById(AuthenticatedCaller caller, Long userId);
    
    /** Query a bounded page of visible users. */
    Result<Pager<UserResponse>> queryUsers(AuthenticatedCaller caller, UserQueryRequest request);
    
    /** Change a user's non-deleted status. */
    Result<Void> updateUserStatus(AuthenticatedCaller caller, Long userId, String status);
    
    /** Soft-delete a user through the deletion-specific use case. */
    Result<Void> deleteUser(AuthenticatedCaller caller, Long userId);
}
