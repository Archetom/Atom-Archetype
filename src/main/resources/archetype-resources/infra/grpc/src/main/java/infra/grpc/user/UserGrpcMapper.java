package ${package}.infra.grpc.user;

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserPageResponse;
import ${package}.api.dto.response.UserResponse;
import ${package}.infra.grpc.user.v1.CreateUserRequest;
import ${package}.infra.grpc.user.v1.ListUsersRequest;
import ${package}.infra.grpc.user.v1.ListUsersResponse;
import ${package}.infra.grpc.user.v1.User;
import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** Maps the User gRPC contract to the public facade DTOs and back. */
final class UserGrpcMapper {

    private UserGrpcMapper() {
    }

    static UserCreateRequest toCreateRequest(CreateUserRequest request) {
        return new UserCreateRequest()
                .setUsername(request.getUsername())
                .setEmail(request.getEmail())
                .setPhoneNumber(request.hasPhoneNumber() ? request.getPhoneNumber() : null)
                .setPassword(request.getPassword())
                .setRealName(request.hasRealName() ? request.getRealName() : null);
    }

    static UserQueryRequest toQueryRequest(ListUsersRequest request) {
        UserQueryRequest query = new UserQueryRequest()
                .setUsername(request.hasUsername() ? request.getUsername() : null)
                .setEmail(request.hasEmail() ? request.getEmail() : null)
                .setStatus(request.hasStatus() ? request.getStatus() : null);
        if (request.hasPage()) {
            query.setPage(request.getPage());
        }
        if (request.hasSize()) {
            query.setSize(request.getSize());
        }
        return query;
    }

    static User toMessage(UserResponse user) {
        User.Builder message = User.newBuilder()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setEmail(user.getEmail())
                .setStatus(user.getStatus())
                .setStatusName(user.getStatusName())
                .setActive(Boolean.TRUE.equals(user.getActive()));
        if (user.getMaskedPhoneNumber() != null) {
            message.setMaskedPhoneNumber(user.getMaskedPhoneNumber());
        }
        if (user.getRealName() != null) {
            message.setRealName(user.getRealName());
        }
        if (user.getCreatedTime() != null) {
            message.setCreatedTime(timestamp(user.getCreatedTime()));
        }
        if (user.getUpdatedTime() != null) {
            message.setUpdatedTime(timestamp(user.getUpdatedTime()));
        }
        return message.build();
    }

    static ListUsersResponse toMessage(UserPageResponse page) {
        List<UserResponse> users = page.getObjectList() == null ? List.of() : page.getObjectList();
        return ListUsersResponse.newBuilder()
                .setPageNum(page.getPageNum())
                .setPageSize(page.getPageSize())
                .setTotalNum(page.getTotalNum())
                .addAllUsers(users.stream().map(UserGrpcMapper::toMessage).toList())
                .build();
    }

    private static Timestamp timestamp(LocalDateTime time) {
        // Audit times are server-zone wall-clock values; convert them with that zone.
        Instant instant = time.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
