package ${package}.infra.rest.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Declares the authorities that guard one area of the HTTP API.
 *
 * <p>{@link SecurityConfig} applies every bean of this type before its catch-all rule, so a
 * route without a declaration still requires authentication. Route rules only reject early:
 * each use case must check the same authority through {@code CallerGuard}.</p>
 */
@FunctionalInterface
public interface ApiRouteAuthorization {

    void authorize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes);
}
