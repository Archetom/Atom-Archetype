package ${package};

import ${package}.infra.grpc.user.v1.CreateUserRequest;
import ${package}.infra.grpc.user.v1.GetUserRequest;
import ${package}.infra.grpc.user.v1.User;
import ${package}.infra.grpc.user.v1.UserServiceGrpc;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** End-to-end gRPC contract: bearer authentication, use-case authorities, and tenant isolation. */
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@TestPropertySource(properties = "atom.grpc.enabled=true")
@AutoConfigureTestGrpcTransport
@Import(TestTokens.TrustedIssuer.class)
class GrpcIntegrationTest extends BaseIntegrationTest {

    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Autowired
    private GrpcChannelFactory channels;

    @Test
    void rejectsUnauthenticatedCalls() {
        assertStatus(Status.Code.UNAUTHENTICATED,
                () -> users(null).getUser(GetUserRequest.newBuilder().setUserId(1L).build()));
    }

    @Test
    void createsAndReadsUsersOnlyInsideTheTokenTenant() {
        UserServiceGrpc.UserServiceBlockingStub tenantA = users(TestTokens.token(7L, 101L, "users:read users:write"));

        User created = tenantA.createUser(validCreateRequest());
        User read = tenantA.getUser(GetUserRequest.newBuilder().setUserId(created.getId()).build());

        assertEquals("grpc_user", read.getUsername());
        assertStatus(Status.Code.NOT_FOUND, () -> users(TestTokens.token(8L, 202L, "users:read"))
                .getUser(GetUserRequest.newBuilder().setUserId(created.getId()).build()));
    }

    @Test
    void enforcesUseCaseAuthorities() {
        assertStatus(Status.Code.PERMISSION_DENIED,
                () -> users(TestTokens.token(7L, 101L, "users:read")).createUser(validCreateRequest()));
    }

    @Override
    protected void initTestData() {
        truncateTable("t_user");
    }

    @Override
    protected void clearTestData() {
        truncateTable("t_user");
    }

    private UserServiceGrpc.UserServiceBlockingStub users(String bearerToken) {
        Metadata headers = new Metadata();
        if (bearerToken != null) {
            headers.put(AUTHORIZATION, "Bearer " + bearerToken);
        }
        return UserServiceGrpc.newBlockingStub(channels.createChannel("default"))
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
    }

    private static CreateUserRequest validCreateRequest() {
        return CreateUserRequest.newBuilder()
                .setUsername("grpc_user")
                .setEmail("grpc_user@example.com")
                .setPassword("long-enough-password")
                .build();
    }

    private static void assertStatus(Status.Code expected, Runnable call) {
        StatusRuntimeException failure = assertThrows(StatusRuntimeException.class, call::run);
        assertEquals(expected, failure.getStatus().getCode());
    }
}
