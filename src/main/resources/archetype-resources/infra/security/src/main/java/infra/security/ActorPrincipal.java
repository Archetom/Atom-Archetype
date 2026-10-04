package ${package}.infra.security;

import java.security.Principal;

/**
 * Verified actor that an inbound transport places in Spring Security's context after
 * authenticating a call, such as the HTTP authentication filter or an RPC server interceptor.
 *
 * <p>This principal is an infrastructure concern. Inbound adapters turn it into an
 * {@code AuthenticatedCaller} through {@link AuthenticatedCallerResolver}; domain and
 * persistence code must not read it through a global or thread-local holder.</p>
 *
 * @param userId authenticated user ID
 * @param tenantId authenticated tenant ID
 */
public record ActorPrincipal(long userId, long tenantId) implements Principal {

    public ActorPrincipal {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be positive");
        }
    }

    @Override
    public String getName() {
        return Long.toString(userId);
    }
}
