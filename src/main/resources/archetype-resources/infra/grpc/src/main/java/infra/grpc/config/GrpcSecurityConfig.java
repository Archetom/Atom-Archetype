package ${package}.infra.grpc.config;

import ${package}.infra.grpc.security.ActorJwtAuthenticationConverter;
import ${package}.infra.grpc.security.GrpcSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Authenticates every gRPC call with a bearer JWT before it reaches a facade.
 *
 * <p>Active only while the gRPC server is enabled ({@code atom.grpc.enabled=true}). A
 * {@link JwtDecoder} is required, for example from
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}; without one, startup fails
 * instead of serving unauthenticated calls. The standard health service stays public for
 * load-balancer probes. Use cases still check authorities through {@code CallerGuard}.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = "spring.grpc.server.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcSecurityProperties.class)
public class GrpcSecurityConfig {

    static final String HEALTH_METHODS = "grpc.health.v1.Health/*";

    @Bean
    @GlobalServerInterceptor
    AuthenticationProcessInterceptor grpcAuthenticationInterceptor(
            GrpcSecurity grpc, JwtDecoder jwtDecoder, GrpcSecurityProperties properties) throws Exception {
        return grpc
                .authorizeRequests(requests -> requests
                        .methods(HEALTH_METHODS).permitAll()
                        .allRequests().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(new ActorJwtAuthenticationConverter(properties))))
                .build();
    }
}
