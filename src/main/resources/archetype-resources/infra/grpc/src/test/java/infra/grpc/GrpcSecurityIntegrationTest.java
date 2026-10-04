package ${package}.infra.grpc;

import ${package}.infra.grpc.config.GrpcSecurityConfig;
import ${package}.infra.security.AuthenticatedCallerResolver;
import ${package}.infra.security.jwt.JwtAuthenticationConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Every gRPC call is authenticated before a facade resolves its caller; health stays public. */
@SpringBootTest(classes = GrpcSecurityIntegrationTest.TestApplication.class,
        properties = "spring.grpc.server.enabled=true")
@AutoConfigureTestGrpcTransport
class GrpcSecurityIntegrationTest {

    private static final KeyPair TRUSTED_KEYS = rsaKeys();
    private static final KeyPair FOREIGN_KEYS = rsaKeys();

    private static final MethodDescriptor<String, String> WHO_AM_I = MethodDescriptor.<String, String>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Probe", "WhoAmI"))
            .setRequestMarshaller(new Utf8Marshaller())
            .setResponseMarshaller(new Utf8Marshaller())
            .build();

    @Autowired
    private GrpcChannelFactory channels;

    @Test
    void rejectsCallsWithoutABearerToken() {
        assertStatus(Status.Code.UNAUTHENTICATED, () -> whoAmI(null));
    }

    @Test
    void rejectsTokensSignedByAnUntrustedKey() {
        assertStatus(Status.Code.UNAUTHENTICATED, () -> whoAmI(token(FOREIGN_KEYS, "7", 11L)));
    }

    @Test
    void rejectsTokensWithoutAUsableActor() {
        assertStatus(Status.Code.UNAUTHENTICATED, () -> whoAmI(token(TRUSTED_KEYS, "alice", 11L)));
    }

    @Test
    void exposesTheVerifiedCallerToFacades() {
        assertEquals("7@11 [orders:read]", whoAmI(token(TRUSTED_KEYS, "7", 11L)));
    }

    @Test
    void keepsTheHealthServicePublicForProbes() {
        HealthCheckResponse response = HealthGrpc.newBlockingStub(channels.createChannel("default"))
                .check(HealthCheckRequest.getDefaultInstance());

        assertEquals(HealthCheckResponse.ServingStatus.SERVING, response.getStatus());
    }

    private String whoAmI(String bearerToken) {
        Metadata headers = new Metadata();
        if (bearerToken != null) {
            headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + bearerToken);
        }
        Channel channel = ClientInterceptors.intercept(
                channels.createChannel("default"), MetadataUtils.newAttachHeadersInterceptor(headers));
        return ClientCalls.blockingUnaryCall(channel, WHO_AM_I, CallOptions.DEFAULT, "");
    }

    private static void assertStatus(Status.Code expected, Runnable call) {
        StatusRuntimeException failure = assertThrows(StatusRuntimeException.class, call::run);
        assertEquals(expected, failure.getStatus().getCode());
    }

    private static String token(KeyPair keys, String subject, long tenantId) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("tenant_id", tenantId)
                    .claim("scope", "orders:read")
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(keys.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static KeyPair rsaKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
    @Import({GrpcSecurityConfig.class, JwtAuthenticationConfig.class, AuthenticatedCallerResolver.class})
    static class TestApplication {

        @Bean
        JwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) TRUSTED_KEYS.getPublic()).build();
        }

        /** Reports the caller that facades would receive, as a facade-backed service does. */
        @Bean
        BindableService probe(AuthenticatedCallerResolver callerResolver) {
            return () -> ServerServiceDefinition.builder("test.Probe")
                    .addMethod(WHO_AM_I, ServerCalls.asyncUnaryCall((request, observer) -> {
                        observer.onNext(callerResolver.currentCaller()
                                .map(caller -> caller.actorId() + "@" + caller.tenantId() + " " + caller.authorities())
                                .orElse("anonymous"));
                        observer.onCompleted();
                    }))
                    .build();
        }
    }

    private static final class Utf8Marshaller implements MethodDescriptor.Marshaller<String> {

        @Override
        public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String parse(InputStream stream) {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
    }
}
