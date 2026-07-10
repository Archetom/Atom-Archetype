package ${package}.infra.rest.config;

import ${package}.infra.rest.security.TrustedHeaderAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * Secure-by-default HTTP configuration.
 *
 * <p>Without an explicitly configured authentication adapter, application APIs
 * reject anonymous requests with HTTP 401. Health remains available for probes;
 * API documentation is public only when its endpoints are enabled.</p>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final String TRUSTED_HEADER_AUTHORITIES =
            "atom.security.trusted-header.authorities";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            org.springframework.beans.factory.ObjectProvider<TrustedHeaderAuthenticationFilter> trustedHeaderFilter
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
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/api/health",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**")
                            .hasAuthority("users:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**")
                            .hasAuthority("users:write")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/**")
                            .hasAuthority("users:write")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**")
                            .hasAuthority("users:delete")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll());

        TrustedHeaderAuthenticationFilter filter = trustedHeaderFilter.getIfAvailable();
        if (filter != null) {
            http.addFilterBefore(filter, AnonymousAuthenticationFilter.class);
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
