package ${package}.infra.rest.security;

import ${package}.application.security.UserAuthorities;
import ${package}.infra.rest.config.ApiRouteAuthorization;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/** Requires the User authorities on the {@code /api/v1/users} routes. */
@Component
public class UserRouteAuthorization implements ApiRouteAuthorization {

    private static final String USER_ROUTES = "/api/v1/users/**";

    @Override
    public void authorize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes) {
        routes.requestMatchers(HttpMethod.GET, USER_ROUTES).hasAuthority(UserAuthorities.READ)
                .requestMatchers(HttpMethod.POST, USER_ROUTES).hasAuthority(UserAuthorities.WRITE)
                .requestMatchers(HttpMethod.PUT, USER_ROUTES).hasAuthority(UserAuthorities.WRITE)
                .requestMatchers(HttpMethod.DELETE, USER_ROUTES).hasAuthority(UserAuthorities.DELETE);
    }
}
