package ${package}.infra.grpc.user;

import ${package}.api.dto.request.QueryRequest;
import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserPageResponse;
import ${package}.api.dto.response.UserResponse;
import ${package}.api.facade.UserFacade;
import ${package}.infra.grpc.support.GrpcFacadeCalls;
import ${package}.infra.grpc.user.v1.CreateUserRequest;
import ${package}.infra.grpc.user.v1.GetUserRequest;
import ${package}.infra.grpc.user.v1.ListUsersRequest;
import ${package}.infra.grpc.user.v1.ListUsersResponse;
import ${package}.infra.grpc.user.v1.UpdateUserStatusRequest;
import ${package}.infra.grpc.user.v1.User;
import ${package}.shared.enums.ApplicationErrorCode;
import ${package}.shared.exception.NonRetryableApplicationException;
import ${package}.shared.util.ResultUtil;
import com.google.protobuf.Empty;
import io.github.archetom.common.result.Result;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserGrpcServiceTest {

    private final RecordingUserFacade facade = new RecordingUserFacade();
    private final UserGrpcService service = new UserGrpcService(facade,
            new GrpcFacadeCalls("grpc-test", Validation.buildDefaultValidatorFactory().getValidator()));

    @Test
    void createsTheUserThroughTheFacadeAndReturnsItsContract() {
        facade.result = success(user());
        RecordingObserver<User> observer = new RecordingObserver<>();

        service.createUser(CreateUserRequest.newBuilder()
                .setUsername("alice")
                .setEmail("alice@example.com")
                .setPassword("long-enough-password")
                .setRealName("Alice")
                .build(), observer);

        UserCreateRequest forwarded = (UserCreateRequest) facade.request;
        assertEquals("alice", forwarded.getUsername());
        assertNull(forwarded.getPhoneNumber());
        User created = observer.values.getFirst();
        assertEquals(42L, created.getId());
        assertEquals("Alice", created.getRealName());
        assertFalse(created.hasMaskedPhoneNumber());
        assertTrue(created.hasCreatedTime());
        assertTrue(observer.completed);
    }

    @Test
    void rejectsInvalidRequestsBeforeTheFacade() {
        RecordingObserver<User> observer = new RecordingObserver<>();

        service.createUser(CreateUserRequest.newBuilder()
                .setEmail("alice@example.com")
                .setPassword("long-enough-password")
                .build(), observer);

        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(observer.error).getCode());
        assertNull(facade.request);
    }

    @Test
    void translatesFacadeFailuresToGrpcStatuses() {
        facade.result = ResultUtil.genErrorResult(new Result<>(), new NonRetryableApplicationException(
                ApplicationErrorCode.RESOURCE_NOT_FOUND, "User does not exist"), "1001", "grpc-test");
        RecordingObserver<User> observer = new RecordingObserver<>();

        service.getUser(GetUserRequest.newBuilder().setUserId(10L).build(), observer);

        Status status = Status.fromThrowable(observer.error);
        assertEquals(Status.Code.NOT_FOUND, status.getCode());
        assertEquals("User does not exist", status.getDescription());
        assertEquals(10L, facade.request);
    }

    @Test
    void listsUsersWithDefaultPagingWhenOmitted() {
        facade.result = success(new UserPageResponse()
                .setPageNum(1).setPageSize(20).setTotalNum(1).setObjectList(List.of(user())));
        RecordingObserver<ListUsersResponse> observer = new RecordingObserver<>();

        service.listUsers(ListUsersRequest.newBuilder().setStatus("ACTIVE").build(), observer);

        UserQueryRequest query = (UserQueryRequest) facade.request;
        assertEquals(QueryRequest.DEFAULT_PAGE, query.getPage());
        assertEquals(QueryRequest.DEFAULT_SIZE, query.getSize());
        assertEquals("ACTIVE", query.getStatus());
        assertEquals(1, observer.values.getFirst().getUsersCount());
    }

    @Test
    void acknowledgesStatusChangesWithAnEmptyResponse() {
        facade.result = success(null);
        RecordingObserver<Empty> observer = new RecordingObserver<>();

        service.updateUserStatus(UpdateUserStatusRequest.newBuilder()
                .setUserId(10L).setStatus("LOCKED").build(), observer);

        assertEquals(List.of(Empty.getDefaultInstance()), observer.values);
        assertEquals("10:LOCKED", facade.request);
    }

    private static UserResponse user() {
        return new UserResponse()
                .setId(42L)
                .setUsername("alice")
                .setEmail("alice@example.com")
                .setRealName("Alice")
                .setStatus("ACTIVE")
                .setStatusName("Active")
                .setActive(true)
                .setCreatedTime(LocalDateTime.now())
                .setUpdatedTime(LocalDateTime.now());
    }

    private static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    /** Records the forwarded request and answers every call with the configured result. */
    @SuppressWarnings("unchecked")
    private static final class RecordingUserFacade implements UserFacade {

        private Object request;
        private Result<?> result;

        @Override
        public Result<UserResponse> createUser(UserCreateRequest request) {
            this.request = request;
            return (Result<UserResponse>) result;
        }

        @Override
        public Result<UserResponse> getUserById(Long userId) {
            this.request = userId;
            return (Result<UserResponse>) result;
        }

        @Override
        public Result<UserPageResponse> queryUsers(UserQueryRequest request) {
            this.request = request;
            return (Result<UserPageResponse>) result;
        }

        @Override
        public Result<Void> updateUserStatus(Long userId, String status) {
            this.request = userId + ":" + status;
            return (Result<Void>) result;
        }

        @Override
        public Result<Void> deleteUser(Long userId) {
            this.request = userId;
            return (Result<Void>) result;
        }
    }

    private static final class RecordingObserver<T> implements StreamObserver<T> {

        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
