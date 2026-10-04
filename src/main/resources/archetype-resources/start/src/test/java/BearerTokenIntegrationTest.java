package ${package};

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.Import;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The assembled application authenticates HTTP requests with bearer JWTs from a trusted issuer. */
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@Import(TestTokens.TrustedIssuer.class)
class BearerTokenIntegrationTest extends BaseIntegrationTest {

    @Test
    void trustedTokenPassesAuthentication() throws Exception {
        // No route matches, so a 404 proves the request was authenticated first.
        mockMvc.perform(get("/api/v1/unmapped").header(AUTHORIZATION, "Bearer " + TestTokens.token(7L, 11L, "")))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingOrUntrustedTokensAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/unmapped"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/unmapped").header(AUTHORIZATION, "Bearer not-a-signed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }
}
