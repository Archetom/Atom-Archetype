package ${package}.infra.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT claims that identify the caller of an HTTP or gRPC request.
 *
 * @param userIdClaim claim holding the positive user ID
 * @param tenantClaim claim holding the positive tenant ID
 * @param authoritiesClaim claim holding authorities as a space-separated string or a list
 */
@ConfigurationProperties(prefix = "atom.security.jwt")
public record JwtClaimsProperties(
        @DefaultValue("sub") String userIdClaim,
        @DefaultValue("tenant_id") String tenantClaim,
        @DefaultValue("scope") String authoritiesClaim) {
}
