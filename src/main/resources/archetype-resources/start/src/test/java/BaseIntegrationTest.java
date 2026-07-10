package ${package};

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test scaffold backed by MySQL Testcontainers.
 *
 * <ul>
 * <li>MySQL managed by Testcontainers</li>
 * <li>dynamic Spring Boot datasource configuration</li>
 * <li>MockMvc HTTP helpers</li>
 * <li>real transaction commits and explicit database cleanup</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {${package}.Bootstrap.class}
        )
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    // ======= Shared Testcontainers configuration =======

    protected static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4.10"))
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withStartupTimeout(Duration.ofMinutes(2));

    static {
        // One JVM-wide container keeps Spring's cached DataSource valid across test classes.
        // Testcontainers' resource reaper stops it when the test JVM exits.
        MYSQL.start();
    }

    /** Return the shared MySQL test container for advanced assertions. */
    protected static MySQLContainer mysqlContainer() { return MYSQL; }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL configuration
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("atom.redis.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    // ======= =======
    @Autowired(required = false)
    protected MockMvc mockMvc;

    @Autowired(required = false)
    protected ObjectMapper objectMapper;

    @Autowired(required = false)
    protected DataSource dataSource;

    // ======= =======
    @BeforeEach
    void setUpIntegration() {
        initTestData();
    }

    @AfterEach
    void tearDownIntegration() {
        clearTestData();
    }

    // ======= HTTP request utility =======
    protected ResultActions performGet(String url, Object... params) throws Exception {
        return mockMvc.perform(withTestActor(get(url, params).contentType(MediaType.APPLICATION_JSON)));
    }

    protected ResultActions performGetWithParams(String url, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder request = get(url);
        params.forEach(request::param);
        return mockMvc.perform(withTestActor(request.contentType(MediaType.APPLICATION_JSON)));
    }

    protected ResultActions performPost(String url, Object requestBody) throws Exception {
        return mockMvc.perform(withTestActor(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requestBody))));
    }

    protected ResultActions performPut(String url, Object requestBody) throws Exception {
        return mockMvc.perform(withTestActor(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requestBody))));
    }

    protected ResultActions performDelete(String url, Object... params) throws Exception {
        return mockMvc.perform(withTestActor(delete(url, params).contentType(MediaType.APPLICATION_JSON)));
    }

    // ======= response utility =======
    protected ResultActions assertSuccess(ResultActions resultActions) throws Exception {
        return resultActions.andExpect(status().isOk());
    }

    protected ResultActions assertValidationError(ResultActions resultActions) throws Exception {
        return resultActions.andExpect(status().isBadRequest());
    }

    // ======= database utility =======
    protected void executeSql(String sql) {
        if (dataSource == null) return;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL: " + sql, e);
        }
    }

    protected void truncateTable(String tableName) {
        executeSql("TRUNCATE TABLE " + tableName);
    }

    // ======= JSON utility =======
    /** object JSON string */
    protected String toJson(Object object) {
        try {
            return objectMapper != null ? objectMapper.writeValueAsString(object) : "";
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /** JSON string object (class) */
    protected <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper != null ? objectMapper.readValue(json, clazz) : null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }

    /** JSON string object (complex, such as List/Map) */
    protected <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper != null ? objectMapper.readValue(json, typeRef) : null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }

    // ======= Test data hooks =======
    protected void initTestData() {
        // Subclasses may override.
    }

    protected void clearTestData() {
        // Subclasses may override.
    }

    protected MockHttpServletRequestBuilder withTestActor(MockHttpServletRequestBuilder request) {
        return request
                .header("X-Dev-User-Id", "888888")
                .header("X-Dev-Tenant-Id", "999999");
    }
}
