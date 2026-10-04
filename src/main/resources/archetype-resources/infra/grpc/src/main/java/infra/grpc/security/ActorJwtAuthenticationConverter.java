package ${package}.infra.grpc.security;

import ${package}.infra.security.ActorPrincipal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maps a verified JWT to the {@link ActorPrincipal} that facades resolve as the caller.
 *
 * <p>The user and tenant claims must hold positive IDs and the authorities claim lists the
 * caller's authorities verbatim. A token without a usable actor or tenant is rejected, so a
 * valid signature alone never grants access.</p>
 */
public class ActorJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final GrpcSecurityProperties properties;

    public ActorJwtAuthenticationConverter(GrpcSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        ActorPrincipal principal = new ActorPrincipal(
                positiveId(jwt, properties.userIdClaim()),
                positiveId(jwt, properties.tenantClaim()));
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities(jwt));
    }

    private static long positiveId(Jwt jwt, String claim) {
        Object value = jwt.getClaims().get(claim);
        if (value != null) {
            try {
                long id = Long.parseLong(value.toString());
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // Rejected below without echoing the claim value.
            }
        }
        throw new InvalidBearerTokenException("Token claim " + claim + " must be a positive ID");
    }

    private List<GrantedAuthority> authorities(Jwt jwt) {
        Stream<String> names = switch (jwt.getClaims().get(properties.authoritiesClaim())) {
            case String text -> Arrays.stream(text.split("\\s+"));
            case Collection<?> values -> values.stream().map(String::valueOf);
            case null, default -> Stream.empty();
        };
        return names.map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
