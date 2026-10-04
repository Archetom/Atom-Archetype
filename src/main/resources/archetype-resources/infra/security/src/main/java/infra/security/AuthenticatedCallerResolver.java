package ${package}.infra.security;

import ${package}.application.security.AuthenticatedCaller;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves the caller that an inbound transport has already authenticated for the current call.
 *
 * <p>HTTP and RPC transports verify the caller's credential and put an {@link ActorPrincipal}
 * with its verified authorities into Spring Security's context on the thread that invokes the
 * facade. Facade adapters read it here and pass it explicitly to use cases, so callers can never
 * supply identity, tenant, or authorities as method arguments.</p>
 */
@Component
public class AuthenticatedCallerResolver {

    /** Returns the verified caller of the current call, or empty when the call is not authenticated. */
    public Optional<AuthenticatedCaller> currentCaller() {
        return from(SecurityContextHolder.getContext().getAuthentication());
    }

    Optional<AuthenticatedCaller> from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof ActorPrincipal principal)) {
            return Optional.empty();
        }

        return Optional.of(new AuthenticatedCaller(
                principal.userId(),
                principal.tenantId(),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toUnmodifiableSet())));
    }
}
