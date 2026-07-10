package ${package};

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
