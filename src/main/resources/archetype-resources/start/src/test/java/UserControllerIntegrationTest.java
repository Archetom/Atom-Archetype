#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserResponse;
import io.github.archetom.common.result.Pager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** End-to-end tests for the User HTTP, security, and persistence contract. */
@DisplayName("User controller integration")
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("OpenAPI documents the User operations and conflict response")
    void openApiDocumentsUserContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/v1/users']['get']['summary']")
                        .value("Query visible users"))
                .andExpect(jsonPath("$['paths']['/api/v1/users']['post']['responses']['409']")
                        .exists());
    }

    @Test
    @DisplayName("anonymous business request - unauthorized")
    void anonymousRequest_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("incomplete trusted identity - unauthorized")
    void incompleteTrustedIdentity_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("X-Dev-User-Id", "888888"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("create user - success")
    void createUser_Success() throws Exception {
        // Given
        UserCreateRequest request = new UserCreateRequest()
            .setUsername("newuser")
            .setEmail("newuser@example.com")
            .setPassword("password1234")
            .setRealName("New User");

        // When
        ResultActions result = performPost("/api/v1/users", request);

        // Then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.username").value("newuser"))
              .andExpect(jsonPath("$.email").value("newuser@example.com"))
              .andExpect(jsonPath("$.realName").value("New User"))
              .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("create user - parameter validation failure")
    void createUser_ValidationFailed() throws Exception {
        // Given
        UserCreateRequest request = new UserCreateRequest()
            .setUsername("") // username is empty
            .setEmail("invalid-email") // email format error
            .setPassword("123"); // password

        // When
        ResultActions result = performPost("/api/v1/users", request);

        // Then
        assertValidationError(result);
    }

    @Test
    @DisplayName("create user - username already exists")
    void createUser_UsernameExists() throws Exception {
        // Given - first create user
        UserCreateRequest firstRequest = new UserCreateRequest()
            .setUsername("existinguser")
            .setEmail("first@example.com")
            .setPassword("password1234")
            .setRealName("First User");
        performPost("/api/v1/users", firstRequest);

        // again create username of user
        UserCreateRequest secondRequest = new UserCreateRequest()
            .setUsername("existinguser") // username
            .setEmail("second@example.com")
            .setPassword("password1234")
            .setRealName("Second User");

        // When
        ResultActions result = performPost("/api/v1/users", secondRequest);

        // Then
        result.andExpect(status().isConflict())
              .andExpect(jsonPath("$.errMsg").value(org.hamcrest.Matchers.containsString("Username already exists")));
    }

    @Test
    @DisplayName("get user by ID - success")
    void getUserById_Success() throws Exception {
        // Given - first create user
        UserCreateRequest createRequest = new UserCreateRequest()
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setPassword("password1234")
            .setRealName("Test User");

        ResultActions createResult = performPost("/api/v1/users", createRequest);
        String responseJson = createResult.andReturn().getResponse().getContentAsString();
        UserResponse createdUser = fromJson(responseJson, UserResponse.class);

        // When
        ResultActions result = performGet("/api/v1/users/{userId}", createdUser.getId());

        // Then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.id").value(createdUser.getId()))
              .andExpect(jsonPath("$.username").value("testuser"))
              .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("get user by ID - hidden from another tenant")
    void getUserById_CrossTenantNotFound() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest()
                .setUsername("tenantuser")
                .setEmail("tenant@example.com")
                .setPassword("password1234")
                .setRealName("Tenant User");
        UserResponse createdUser = fromJson(
                performPost("/api/v1/users", createRequest)
                        .andReturn().getResponse().getContentAsString(),
                UserResponse.class);

        mockMvc.perform(get("/api/v1/users/{userId}", createdUser.getId())
                        .header("X-Dev-User-Id", "888888")
                        .header("X-Dev-Tenant-Id", "123456"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("get user by ID - user does not exist")
    void getUserById_UserNotFound() throws Exception {
        // When
        ResultActions result = performGet("/api/v1/users/{userId}", 99999L);

        // Then
        result.andExpect(status().isNotFound())
              .andExpect(jsonPath("$.errMsg").value(org.hamcrest.Matchers.containsString("User does not exist")));
    }

    @Test
    @DisplayName("query users - success")
    void queryUsers_Success() throws Exception {
        // Given - create Test User
        for (int i = 1; i <= 3; i++) {
            UserCreateRequest request = new UserCreateRequest()
                .setUsername("user" + i)
                .setEmail("user" + i + "@example.com")
                .setPassword("password1234")
                .setRealName("User " + i);
            performPost("/api/v1/users", request);
        }

        // When
        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("size", "10");

        ResultActions result = performGetWithParams("/api/v1/users", params);

        // Then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.totalNum").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
              .andExpect(jsonPath("$.objectList").isArray());
    }

    @Test
    @DisplayName("query users - username filter")
    void queryUsers_FilterByUsername() throws Exception {
        // Given - create Test User
        UserCreateRequest request = new UserCreateRequest()
            .setUsername("filteruser")
            .setEmail("filter@example.com")
            .setPassword("password1234")
            .setRealName("Filter User");
        performPost("/api/v1/users", request);

        // When
        Map<String, String> params = new HashMap<>();
        params.put("username", "filteruser");
        params.put("page", "1");
        params.put("size", "10");

        ResultActions result = performGetWithParams("/api/v1/users", params);

        // Then
        result.andExpect(status().isOk());

        String responseJson = result.andReturn().getResponse().getContentAsString();
        Pager<UserResponse> pager = fromJson(responseJson, new TypeReference<Pager<UserResponse>>() {});

        assertTrue(pager.getTotalNum() >= 1);
        assertTrue(pager.getObjectList().stream()
            .anyMatch(user -> "filteruser".equals(user.getUsername())));
    }

    @Test
    @DisplayName("query users - rejects an unbounded page size")
    void queryUsers_RejectsOversizedPage() throws Exception {
        performGetWithParams("/api/v1/users", Map.of("page", "1", "size", "201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("update user status - success")
    void updateUserStatus_Success() throws Exception {
        // Given - first create user
        UserCreateRequest createRequest = new UserCreateRequest()
            .setUsername("statususer")
            .setEmail("status@example.com")
            .setPassword("password1234")
            .setRealName("Status User");

        ResultActions createResult = performPost("/api/v1/users", createRequest);
        String responseJson = createResult.andReturn().getResponse().getContentAsString();
        UserResponse createdUser = fromJson(responseJson, UserResponse.class);

        // When
        ResultActions result = mockMvc.perform(withTestActor(
            put("/api/v1/users/{userId}/status", createdUser.getId())
                .param("status", "INACTIVE")
        ));

        // Then
        result.andExpect(status().isOk());

        // validate status already update
        ResultActions getResult = performGet("/api/v1/users/{userId}", createdUser.getId());
        getResult.andExpect(status().isOk())
                 .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("update user status - invalid status")
    void updateUserStatus_InvalidStatus() throws Exception {
        // Given - first create user
        UserCreateRequest createRequest = new UserCreateRequest()
            .setUsername("invalidstatususer")
            .setEmail("invalidstatus@example.com")
            .setPassword("password1234")
            .setRealName("Invalid Status User");

        ResultActions createResult = performPost("/api/v1/users", createRequest);
        String responseJson = createResult.andReturn().getResponse().getContentAsString();
        UserResponse createdUser = fromJson(responseJson, UserResponse.class);

        // When
        ResultActions result = mockMvc.perform(withTestActor(
            put("/api/v1/users/{userId}/status", createdUser.getId())
                .param("status", "INVALID_STATUS")
        ));

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("update user status - cannot bypass deletion use case")
    void updateUserStatus_RejectsDeleted() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest()
                .setUsername("statusdeleteuser")
                .setEmail("statusdelete@example.com")
                .setPassword("password1234")
                .setRealName("Status Delete User");
        UserResponse createdUser = fromJson(
                performPost("/api/v1/users", createRequest)
                        .andReturn().getResponse().getContentAsString(),
                UserResponse.class);

        mockMvc.perform(withTestActor(
                        put("/api/v1/users/{userId}/status", createdUser.getId())
                                .param("status", "DELETED")))
                .andExpect(status().isUnprocessableContent());

        performGet("/api/v1/users/{userId}", createdUser.getId())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("delete user - success")
    void deleteUser_Success() throws Exception {
        // Given - first create user
        UserCreateRequest createRequest = new UserCreateRequest()
            .setUsername("deleteuser")
            .setEmail("delete@example.com")
            .setPassword("password1234")
            .setRealName("Delete User");

        ResultActions createResult = performPost("/api/v1/users", createRequest);
        String responseJson = createResult.andReturn().getResponse().getContentAsString();
        UserResponse createdUser = fromJson(responseJson, UserResponse.class);

        // When
        ResultActions result = performDelete("/api/v1/users/{userId}", createdUser.getId());

        // Then
        result.andExpect(status().isOk());

        // Soft-deleted users are no longer visible through the public read API.
        ResultActions getResult = performGet("/api/v1/users/{userId}", createdUser.getId());
        getResult.andExpect(status().isNotFound());

        // Repeated deletion follows the same not-found visibility policy.
        performDelete("/api/v1/users/{userId}", createdUser.getId())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("delete user - user does not exist")
    void deleteUser_UserNotFound() throws Exception {
        // When
        ResultActions result = performDelete("/api/v1/users/{userId}", 99999L);

        // Then
        result.andExpect(status().isNotFound())
              .andExpect(jsonPath("$.errMsg").value(org.hamcrest.Matchers.containsString("User does not exist")));
    }

    @Override
    protected void initTestData() {
        // clean test data
        truncateTable("t_user");
    }

    @Override
    protected void clearTestData() {
        // clean test data
        truncateTable("t_user");
    }
}
