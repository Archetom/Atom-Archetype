package ${package}.infra.rest.config;

import ${package}.infra.security.AuthenticatedCallerResolver;
import ${package}.infra.security.jwt.JwtAuthenticationConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP requests authenticate with bearer JWTs, mapped to the same caller as gRPC calls. */
@SpringBootTest(classes = BearerTokenAuthenticationTest.TestApplication.class)
@AutoConfigureMockMvc
class BearerTokenAuthenticationTest {

    private static final KeyPair TRUSTED_KEYS = rsaKeys();
    private static final KeyPair FOREIGN_KEYS = rsaKeys();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/caller"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokensSignedByAnUntrustedKey() throws Exception {
        mockMvc.perform(get("/api/caller").header(AUTHORIZATION, bearer(FOREIGN_KEYS, "7")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(WWW_AUTHENTICATE, containsString("Bearer")));
    }

    @Test
    void rejectsTokensWithoutAUsableActor() throws Exception {
        mockMvc.perform(get("/api/caller").header(AUTHORIZATION, bearer(TRUSTED_KEYS, "alice")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resolvesTheVerifiedCallerFromTheToken() throws Exception {
        mockMvc.perform(get("/api/caller").header(AUTHORIZATION, bearer(TRUSTED_KEYS, "7")))
                .andExpect(status().isOk())
                .andExpect(content().string("7@11 [orders:read]"));
    }

    @Test
    void appliesRouteAuthorizationsBeforeTheAuthenticatedFallback() throws Exception {
        mockMvc.perform(get("/api/caller/admin").header(AUTHORIZATION, bearer(TRUSTED_KEYS, "7")))
                .andExpect(status().isForbidden());
    }

    private static String bearer(KeyPair keys, String subject) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .claim("tenant_id", 11L)
                    .claim("scope", "orders:read")
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(keys.getPrivate()));
            return "Bearer " + jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static KeyPair rsaKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
    @Import({SecurityConfig.class, JwtAuthenticationConfig.class, AuthenticatedCallerResolver.class,
            CallerController.class})
    static class TestApplication {

        @Bean
        JwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) TRUSTED_KEYS.getPublic()).build();
        }

        @Bean
        ApiRouteAuthorization adminRoutes() {
            return routes -> routes.requestMatchers("/api/caller/admin").hasAuthority("orders:admin");
        }
    }

    /** Reports the caller that facades would receive for this request. */
    @RestController
    static class CallerController {

        private final AuthenticatedCallerResolver callerResolver;

        CallerController(AuthenticatedCallerResolver callerResolver) {
            this.callerResolver = callerResolver;
        }

        @GetMapping("/api/caller")
        String caller() {
            return callerResolver.currentCaller()
                    .map(caller -> caller.actorId() + "@" + caller.tenantId() + " " + caller.authorities())
                    .orElse("anonymous");
        }

        @GetMapping("/api/caller/admin")
        String admin() {
            return "admin";
        }
    }
}
