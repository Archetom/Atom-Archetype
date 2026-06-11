package ${package};

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * integration test class (scaffold, support MySQL + Redis Testcontainers,)
 *
 * <ul>
 * <li>Testcontainers MySQL + Redis</li>
 * <li> to Spring Boot test configuration </li>
 * <li>MockMvc & RestTemplate test HTTP</li>
 * <li> database /Redis convenience encapsulate </li>
 * <li> full transaction & data clean </li>
 * <li> support complex class JSON column </li>
 * <li> can define container, support </li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {${package}.Bootstrap.class}
        )
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    // ======= Testcontainers container configuration (can in class define) =======

    @Container
    protected static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("sql/init-test-data.sql") // optional, scaffold can remove
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    protected static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofMinutes(1));

    /** get MySQL test container, can used for define / */
    protected static MySQLContainer<?> mysqlContainer() { return mysql; }

    /** get Redis test container, can used for define / */
    protected static GenericContainer<?> redisContainer() { return redis; }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL configuration
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        // test configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("logging.level.org.springframework.web", () -> "DEBUG");
        registry.add("logging.level.${package}", () -> "DEBUG");
    }

    // ======= =======
    @Autowired(required = false)
    protected MockMvc mockMvc;

    @Autowired(required = false)
    protected TestRestTemplate restTemplate;

    @Autowired(required = false)
    protected ObjectMapper objectMapper;

    @Autowired(required = false)
    protected DataSource dataSource;

    @Autowired(required = false)
    protected StringRedisTemplate redisTemplate;

    @LocalServerPort
    protected int port;

    // ======= =======
    @BeforeEach
    void setUpIntegration() {
        clearRedisData();
        initTestData();
    }

    @AfterEach
    void tearDownIntegration() {
        clearTestData();
        clearRedisData();
    }

    // ======= HTTP request utility =======
    protected ResultActions performGet(String url, Object... params) throws Exception {
        return mockMvc.perform(get(url, params).contentType(MediaType.APPLICATION_JSON)).andDo(print());
    }

    protected ResultActions performGetWithParams(String url, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder request = get(url);
        params.forEach(request::param);
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andDo(print());
    }

    protected ResultActions performPost(String url, Object requestBody) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requestBody))).andDo(print());
    }

    protected ResultActions performPut(String url, Object requestBody) throws Exception {
        return mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(requestBody))).andDo(print());
    }

    protected ResultActions performDelete(String url, Object... params) throws Exception {
        return mockMvc.perform(delete(url, params).contentType(MediaType.APPLICATION_JSON)).andDo(print());
    }

    protected ResultActions performPostWithAuth(String url, Object requestBody, String token) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(toJson(requestBody))).andDo(print());
    }

    // ======= response utility =======
    protected ResultActions assertSuccess(ResultActions resultActions) throws Exception {
        return resultActions.andExpect(status().isOk());
    }

    protected ResultActions assertBusinessError(ResultActions resultActions, String expectedErrorCode) throws Exception {
        return resultActions
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errCode").value(expectedErrorCode));
    }

    protected ResultActions assertValidationError(ResultActions resultActions) throws Exception {
        return resultActions.andExpect(status().isUnprocessableEntity());
    }

    protected ResultActions assertResponseData(ResultActions resultActions, String jsonPath, Object expectedValue) throws Exception {
        return resultActions.andExpect(jsonPath(jsonPath).value(expectedValue));
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

    protected void insertTestData(String tableName, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder(" VALUES (");
        data.forEach((key, value) -> {
            sql.append(key).append(",");
            values.append(value instanceof String ? "'" + value + "'" : value).append(",");
        });
        sql.setLength(sql.length() - 1);
        values.setLength(values.length() - 1);
        sql.append(")").append(values).append(")");
        executeSql(sql.toString());
    }

    // ======= Redis utility =======
    protected void setRedisValue(String key, String value) {
        if (redisTemplate != null) redisTemplate.opsForValue().set(key, value);
    }

    protected void setRedisValue(String key, String value, Duration timeout) {
        if (redisTemplate != null) redisTemplate.opsForValue().set(key, value, timeout);
    }

    protected String getRedisValue(String key) {
        return redisTemplate != null ? redisTemplate.opsForValue().get(key) : null;
    }

    /** security Redis data, without Redis skip */
    protected void clearRedisData() {
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.getConnectionFactory().getConnection().flushAll();
            }
        } catch (Exception ignored) {}
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

    // ======= test data (can override) =======
    protected void initTestData() {
        // class can override
    }

    protected void clearTestData() {
        // class can override
    }

    // ======= convenience utility method =======
    /** etc. async */
    protected void waitForAsyncOperation(Duration timeout) {
        try {
            Thread.sleep(timeout.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting", e);
        }
    }

    /** get full URL */
    protected String getFullUrl(String path) {
        return "http://localhost:" + port + path;
    }

    /** set Test User context (sample, actual business) */
    protected void setTestUserContext(String userId, String tenantId) {
        // can ThreadLocal or security context set simulate user
    }
}
