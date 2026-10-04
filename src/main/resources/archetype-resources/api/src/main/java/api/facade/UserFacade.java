#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.facade;

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserPageResponse;
import ${package}.api.dto.response.UserResponse;
import io.github.archetom.common.result.Result;

/**
 * Public facade contract for the bundled User example, shared by HTTP and RPC clients.
 *
 * <p>Methods never accept identity, tenant, or authorities. The server resolves the caller
 * from the transport's verified authentication, so a client cannot act as another user or
 * select another tenant through arguments.</p>
 */
public interface UserFacade {

    /** Create a user inside the caller's tenant. */
    Result<UserResponse> createUser(UserCreateRequest request);

    /** Get a visible user by tenant-scoped ID. */
    Result<UserResponse> getUserById(Long userId);

    /** Query a bounded page of visible users. */
    Result<UserPageResponse> queryUsers(UserQueryRequest request);

    /** Change a user's non-deleted status. */
    Result<Void> updateUserStatus(Long userId, String status);

    /** Soft-delete a user through the deletion-specific use case. */
    Result<Void> deleteUser(Long userId);
}
