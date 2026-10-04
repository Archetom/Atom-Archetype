package ${package}.infra.grpc.security;

import ${package}.infra.security.ActorPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActorJwtAuthenticationConverterTest {

    private final ActorJwtAuthenticationConverter converter =
            new ActorJwtAuthenticationConverter(new GrpcSecurityProperties("sub", "tenant_id", "scope"));

    @Test
    void mapsVerifiedClaimsToTheActorAndItsAuthorities() {
        AbstractAuthenticationToken authentication = converter.convert(
                jwt(Map.of("sub", "7", "tenant_id", 11L, "scope", "orders:read orders:write")));

        assertTrue(authentication.isAuthenticated());
        assertEquals(new ActorPrincipal(7L, 11L), authentication.getPrincipal());
        assertEquals(Set.of("orders:read", "orders:write"), authorities(authentication));
    }

    @Test
    void acceptsAuthoritiesAsAListAndConfiguredClaimNames() {
        ActorJwtAuthenticationConverter custom = new ActorJwtAuthenticationConverter(
                new GrpcSecurityProperties("uid", "org", "roles"));

        AbstractAuthenticationToken authentication = custom.convert(
                jwt(Map.of("uid", 7, "org", "11", "roles", List.of("orders:read"))));

        assertEquals(new ActorPrincipal(7L, 11L), authentication.getPrincipal());
        assertEquals(Set.of("orders:read"), authorities(authentication));
    }

    @Test
    void grantsNoAuthoritiesWhenTheClaimIsMissing() {
        AbstractAuthenticationToken authentication = converter.convert(jwt(Map.of("sub", "7", "tenant_id", 11L)));

        assertTrue(authorities(authentication).isEmpty());
    }

    @Test
    void rejectsTokensWithoutAPositiveActorOrTenant() {
        assertRejected(Map.of("sub", "alice", "tenant_id", 11L));
        assertRejected(Map.of("sub", "7"));
        assertRejected(Map.of("sub", "7", "tenant_id", 0));
        assertRejected(Map.of("sub", "-7", "tenant_id", 11L));
    }

    private void assertRejected(Map<String, Object> claims) {
        InvalidBearerTokenException failure = assertThrows(
                InvalidBearerTokenException.class, () -> converter.convert(jwt(claims)));
        assertFalse(failure.getMessage().contains("alice"));
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claims(values -> values.putAll(claims))
                .build();
    }

    private static Set<String> authorities(AbstractAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
