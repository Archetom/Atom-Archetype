package ${package}.infra.rest.config;

import ${package}.infra.rest.security.TrustedHeaderAuthenticationFilter;
import ${package}.infra.security.jwt.ActorJwtAuthenticationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * Secure-by-default HTTP configuration.
 *
 * <p>Requests authenticate with a bearer JWT whenever a {@link JwtDecoder} is configured,
 * for example through {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}, using
 * the same claim mapping as gRPC. The trusted-header adapter is an additional dev/test
 * option. Without any authentication adapter, application APIs reject every request
 * with HTTP 401. Health remains available for probes; API documentation is public only
 * when its endpoints are enabled.</p>
 *
 * <p>Route authorities come from {@link ApiRouteAuthorization} beans. Any other
 * {@code /api/**} route requires authentication, and everything else is denied.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final String TRUSTED_HEADER_AUTHORITIES =
            "atom.security.trusted-header.authorities";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<TrustedHeaderAuthenticationFilter> trustedHeaderFilter,
            ObjectProvider<JwtDecoder> jwtDecoder,
            ObjectProvider<ActorJwtAuthenticationConverter> jwtAuthenticationConverter,
            ObjectProvider<ApiRouteAuthorization> routeAuthorizations
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(unauthorizedEntryPoint()))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(
                            "/actuator/health",
                            "/actuator/health/**",
                            "/api/health",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/v3/api-docs.yaml"
                    ).permitAll();
                    routeAuthorizations.orderedStream().forEach(routes -> routes.authorize(authorize));
                    authorize.requestMatchers("/api/**").authenticated()
                            .anyRequest().denyAll();
                });

        JwtDecoder decoder = jwtDecoder.getIfAvailable();
        if (decoder != null) {
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                    .decoder(decoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter.getObject())));
        }

        TrustedHeaderAuthenticationFilter filter = trustedHeaderFilter.getIfAvailable();
        if (filter != null) {
            http.addFilterBefore(filter, AnonymousAuthenticationFilter.class);
        }

        if (decoder == null && filter == null) {
            log.warn("No request authentication is configured, so every API request is rejected with HTTP 401. "
                    + "Configure a JWT decoder, for example spring.security.oauth2.resourceserver.jwt.issuer-uri.");
        }
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Bean
    @Profile("(dev | test) & !prod")
    @ConditionalOnProperty(
            prefix = "atom.security.trusted-header",
            name = "enabled",
            havingValue = "true"
    )
    public TrustedHeaderAuthenticationFilter trustedHeaderAuthenticationFilter(Environment environment) {
        return new TrustedHeaderAuthenticationFilter(configuredAuthorities(environment));
    }

    /**
     * A Filter bean is otherwise registered by the servlet container in
     * addition to Spring Security. Disable that registration so it runs once,
     * at the deliberate position in the security chain.
     */
    @Bean
    @Profile("(dev | test) & !prod")
    @ConditionalOnProperty(
            prefix = "atom.security.trusted-header",
            name = "enabled",
            havingValue = "true"
    )
    public FilterRegistrationBean<TrustedHeaderAuthenticationFilter> trustedHeaderFilterRegistration(
            TrustedHeaderAuthenticationFilter filter
    ) {
        FilterRegistrationBean<TrustedHeaderAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private List<String> configuredAuthorities(Environment environment) {
        String value = environment.getProperty(TRUSTED_HEADER_AUTHORITIES, "");
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(authority -> !authority.isEmpty())
                .distinct()
                .toList();
    }
}
