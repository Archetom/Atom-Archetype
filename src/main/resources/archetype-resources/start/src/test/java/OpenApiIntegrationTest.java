package ${package};

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies that the generated machine-readable API contract is actually served. */
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class OpenApiIntegrationTest extends BaseIntegrationTest {

    @Test
    void publishesProjectIdentityAndVersion() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.info.title").value("${rootArtifactId}-test API"))
                .andExpect(jsonPath("$.info.version").value("${version}"))
                .andExpect(jsonPath("$['components']['securitySchemes']['devUserId']['name']")
                        .value("X-Dev-User-Id"))
                .andExpect(jsonPath("$['components']['securitySchemes']['devTenantId']['name']")
                        .value("X-Dev-Tenant-Id"));
    }
}
