#set( $dollar = '$' )
package ${package};

import ${package}.infra.rest.logging.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class HealthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void redisDisabledDoesNotDegradeApplicationHealth() throws Exception {
        assertFalse(environment.getProperty("atom.redis.enabled", Boolean.class, true));
        assertFalse(environment.getProperty("management.health.redis.enabled", Boolean.class, true));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("${dollar}.status").value("UP"));
    }

    @Test
    void everyResponseCarriesRequestIdEvenWhenSecurityRejectsIt() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().exists(RequestIdFilter.HEADER));
        mockMvc.perform(get("/api/v1/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestIdFilter.HEADER));
        mockMvc.perform(get("/actuator/health").header(RequestIdFilter.HEADER, "gateway-42"))
                .andExpect(header().string(RequestIdFilter.HEADER, "gateway-42"));
    }
}
