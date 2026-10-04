package ${package}.infra.security;

import ${package}.application.security.AuthenticatedCaller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedCallerResolverTest {

    private final AuthenticatedCallerResolver resolver = new AuthenticatedCallerResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesTheVerifiedPrincipalOfTheCurrentCall() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new ActorPrincipal(7L, 11L),
                null,
                List.of(new SimpleGrantedAuthority("users:read"))));
        SecurityContextHolder.setContext(context);

        AuthenticatedCaller caller = resolver.currentCaller().orElseThrow();

        assertEquals(7L, caller.actorId());
        assertEquals(11L, caller.tenantId());
        assertEquals(Set.of("users:read"), caller.authorities());
    }

    @Test
    void mapsAuthorityValuesRatherThanTheirDisplayText() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                new ActorPrincipal(7L, 11L),
                null,
                List.of(new DisplayOnlyAuthority()));

        AuthenticatedCaller caller = resolver.from(authentication).orElseThrow();

        assertEquals(Set.of("users:read"), caller.authorities());
    }

    @Test
    void resolvesNothingForMissingUnauthenticatedOrUnexpectedPrincipals() {
        assertTrue(resolver.currentCaller().isEmpty());
        assertTrue(resolver.from(null).isEmpty());
        assertTrue(resolver.from(
                new UsernamePasswordAuthenticationToken(new ActorPrincipal(7L, 11L), null)).isEmpty());
        assertTrue(resolver.from(
                UsernamePasswordAuthenticationToken.authenticated("actor", null, List.of())).isEmpty());
    }

    private static final class DisplayOnlyAuthority implements GrantedAuthority {

        @Override
        public String getAuthority() {
            return "users:read";
        }

        @Override
        public String toString() {
            return "display-only authority";
        }
    }
}
