package ${package}.infra.grpc.user;

import ${package}.api.facade.UserFacade;
import ${package}.infra.grpc.support.GrpcFacadeCalls;
import ${package}.infra.grpc.user.v1.CreateUserRequest;
import ${package}.infra.grpc.user.v1.DeleteUserRequest;
import ${package}.infra.grpc.user.v1.GetUserRequest;
import ${package}.infra.grpc.user.v1.ListUsersRequest;
import ${package}.infra.grpc.user.v1.ListUsersResponse;
import ${package}.infra.grpc.user.v1.UpdateUserStatusRequest;
import ${package}.infra.grpc.user.v1.User;
import ${package}.infra.grpc.user.v1.UserServiceGrpc;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

/**
 * gRPC adapter for the bundled User facade.
 *
 * <p>The bearer token authenticates the call; the facade resolves the caller and the use case
 * checks its authorities, exactly as for HTTP requests.</p>
 */
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserFacade userFacade;
    private final GrpcFacadeCalls calls;

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<User> responseObserver) {
        calls.unary(responseObserver, () -> UserGrpcMapper.toMessage(calls.unwrap(
                userFacade.createUser(calls.validated(UserGrpcMapper.toCreateRequest(request))))));
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<User> responseObserver) {
        calls.unary(responseObserver, () -> UserGrpcMapper.toMessage(calls.unwrap(
                userFacade.getUserById(request.getUserId()))));
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        calls.unary(responseObserver, () -> UserGrpcMapper.toMessage(calls.unwrap(
                userFacade.queryUsers(calls.validated(UserGrpcMapper.toQueryRequest(request))))));
    }

    @Override
    public void updateUserStatus(UpdateUserStatusRequest request, StreamObserver<Empty> responseObserver) {
        calls.unary(responseObserver, () -> {
            calls.unwrap(userFacade.updateUserStatus(request.getUserId(), request.getStatus()));
            return Empty.getDefaultInstance();
        });
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
        calls.unary(responseObserver, () -> {
            calls.unwrap(userFacade.deleteUser(request.getUserId()));
            return Empty.getDefaultInstance();
        });
    }
}
