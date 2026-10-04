package ${package}.infra.security.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shares one JWT claim mapping between HTTP and gRPC.
 *
 * <p>Token verification itself comes from Spring Boot's resource-server properties, such as
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}; transports use bearer tokens only
 * when a {@code JwtDecoder} is configured.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtClaimsProperties.class)
public class JwtAuthenticationConfig {

    @Bean
    ActorJwtAuthenticationConverter actorJwtAuthenticationConverter(JwtClaimsProperties properties) {
        return new ActorJwtAuthenticationConverter(properties);
    }
}
