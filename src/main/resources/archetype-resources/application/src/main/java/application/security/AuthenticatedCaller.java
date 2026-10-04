package ${package}.application.security;

import java.util.Set;

/**
 * Identity and tenant established by a trusted authentication mechanism.
 *
 * <p>This type is server-side use-case context, never part of an HTTP or RPC contract.
 * Inbound adapters resolve it from the transport's verified authentication; it must never
 * be populated from request data or from untrusted client-supplied headers.</p>
 */
public record AuthenticatedCaller(Long actorId, Long tenantId, Set<String> authorities) {

    public AuthenticatedCaller {
        if (actorId == null || actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
    }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }
}
