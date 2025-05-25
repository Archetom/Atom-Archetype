#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.dto.response.UserResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.archetom.common.result.Pager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 用户控制器集成测试
 * @author hanfeng
 */
@DisplayName("用户控制器集成测试")
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class UserControllerIntegrationTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("创建用户 - 成功")
    void createUser_Success() throws Exception {
        // Given
        UserCreateRequest request = new UserCreateRequest()
            .setUsername("newuser")
            .setEmail("newuser@example.com")
            .setPassword("password123")
            .setRealName("New User");
        
        // When
        ResultActions result = performPost("/api/users", request);
        
        // Then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.username").value("newuser"))
              .andExpect(jsonPath("$.email").value("newuser@example.com"))
              .andExpect(jsonPath("$.realName").value("New User"))
              .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    
    @Test
    @DisplayName("创建用户 - 参数校验失败")
    void createUser_ValidationFailed() throws Exception {
        // Given
        UserCreateRequest request = new UserCreateRequest()
            .setUsername("") // 用户名为空
            .setEmail("invalid-email") // 邮箱格式错误
            .setPassword("123"); // 密码太短
        
        // When
        ResultActions result = performPost("/api/users", request);
        
        // Then
        assertValidationError(result);
    }
    
    @Test
    @DisplayName("创建用户 - 用户名已存在")
    void createUser_UsernameExists() throws Exception {
        // Given - 先创建一个用户
        UserCreateRequest firstRequest = new UserCreateRequest()
            .setUsername("existinguser")
            .setEmail("first@example.com")
            .setPassword("password123")
            .setRealName("First User");
        performPost("/api/users", firstRequest);
        
        // 再次创建相同用户名的用户
        UserCreateRequest secondRequest = new UserCreateRequest()
            .setUsername("existinguser") // 相同用户名
            .setEmail("second@example.com")
            .setPassword("password123")
            .setRealName("Second User");
        
        // When
        ResultActions result = performPost("/api/users", secondRequest);
        
        // Then
        result.andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.errMsg").value(org.hamcrest.Matchers.containsString("用户名已存在")));
    }
    
    @Test
    @DisplayName("根据ID获取用户 - 成功")
    void getUserById_Success() throws Exception {
        // Given - 先创建一个用户
        UserCreateRequest createRequest = new UserCreateRequest()
            .setUsername("testuser")
            .setEmail("test@example.com")
            .setPassword("password123")
            .setRealName("Test User");
        
        ResultActions createResult = performPost("/api/users", createRequest);
        String responseJson = createResult.andReturn().getResponse().getContentAsString();
        UserResponse createdUser = fromJson(responseJson, UserResponse.class);
        
        // When
        ResultActions result = performGet("/api/users/{userId}", createdUser.getId());
        
        // Then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.id").value(createdUser.getId()))
              .andExpect(jsonPath("$.username").value("testuser"))
              .andExpect(jsonPath("$.email").value("test@example.com"));
    }
    
    @Test
    @DisplayName("根据ID获取用户 - 用户不存在")
    void getUserById_UserNotFound() throws Exception {
        // When
        ResultActions result = performGet("/api/users/{userId}", 99999L);
        
        // Then
        result.andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.errMsg").value(org.hamcrest.Matchers.containsString("用户不存在")));
    }
    
    @Test
    @DisplayName("分页查询用户 - 成功")
    void queryUsers_Success() throws Exception {
        // Given - 创建几个测试用户
        for (int i = 1; i <= 3; i++) {
            UserCreateRequest request = new UserCreateRequest()
                .setUsername("user" + i)
                .setEmail("user" + i + "@example.com")
                .setPassword("password123")
                .setRealName("User " + i);
            performPost("/api/users", request);
        }
        
        // When
        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("size", "10");
        
        ResultActions result = performGetWithParams("/api/users", params);
        
        // Then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.totalNum").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
              .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    @DisplayName("分页查询用户 - 按用户名筛选")
    void queryUsers_FilterByUsername() throws Exception {
        // Given - 创建测试用户
        UserCreateRequest request = new UserCreateRequest()
            .setUsername("filteruser")
            .setEmail("filter@example.com")
            .setPassword("password123")
            .setRealName("Filter User");
        performPost("/api/users", request);
        
        // When
        Map<String, String> params = new HashMap<>();
        params.put("username", "filteruser");
        params.put("page", "1");
        params.put("size", "10");
        
        ResultActions result = performGetWithParams("/api/users", params);
        
        // Then
        result.andExpect(status().isOk());
        
        String responseJson = result.andReturn().getResponse().getContentAsString();
        Pager<UserResponse> pager = fromJson(responseJson, new TypeReference<Pager<UserResponse>>() {});
        
        assertTrue(pager.getTotalNum() >= 1);
        assertTrue(pager.getObjectList().stream()
            .anyMatch(user -> "filteruser".equals(user.getUsername())));
    }
    
    @Test
    @DisplayName("更新用户状态 - 成功")
    void updateUserStatus_Success() throws Exception {
        // Given - 先创建一个用户
        UserCreateRequest createRequest = new UserCreateRequest()
            .setUsername("statususer")
            .setEmail("status@example.com")
            .setPassword("password123")
            .setRealName("Status User");
        
        ResultActions createResult = performPost("/api/users", createRequest);
        String responseJson = createResult.andReturn().getResponse().getContentAsString();
        UserResponse createdUser = fromJson(responseJson, UserResponse.class);
        
        // When
        ResultActions result = mockMvc.perform(
            put("/api/users/{userId}/status", createdUser.getId())
                .param("status", "INACTIVE")
        );
        
        // Then
        result.andExpect(status().isOk());
        
        // 验证状态已更新
        ResultActions getResult = performGet("/api/users/{userId}", createdUser.getId());
        getResult.andExpect(status().isOk())
                 .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
    
    @Test
    @DisplayName("更新用户状态 - 无效状态")
    void updateUserStatus_InvalidStatus() throws Exception {
        // Given - 先创建一个用户
        UserCreateRequest createRequest = new UserCreateRequest()
            .setUsername("invalidstatususer")
            .setEmail("invalidstatus@example.com")
            .setPassword("password123")
            .setRealName("Invalid Status User");
        
        ResultActions createResult = performPost("/api/users", createRequest);
        String responseJson = createResult.andReturn().getResponse().getContentAsString();
        UserResponse createdUser = fromJson(responseJson, UserResponse.class);
        
        // When
        ResultActions result = mockMvc.perform(
            put("/api/users/{userId}/status", createdUser.getId())
                .param("status", "INVALID_STATUS")
        );
        
        // Then
        result.andExpect(status().isUnprocessableEntity());
    }
    
    @Test
    @DisplayName("删除用户 - 成功")
    void deleteUser_Success() throws Exception {
        // Given - 先创建一个用户
        UserCreateRequest createRequest = new UserCreateRequest()
            .setUsername("deleteuser")
            .setEmail("delete@example.com")
            .setPassword("password123")
            .setRealName("Delete User");
        
        ResultActions createResult = performPost("/api/users", createRequest);
        String responseJson = createResult.andReturn().getResponse().getContentAsString();
        UserResponse createdUser = fromJson(responseJson, UserResponse.class);
        
        // When
        ResultActions result = performDelete("/api/users/{userId}", createdUser.getId());
        
        // Then
        result.andExpect(status().isOk());
        
        // 验证用户状态已变为DELETED
        ResultActions getResult = performGet("/api/users/{userId}", createdUser.getId());
        getResult.andExpect(status().isOk())
                 .andExpect(jsonPath("$.status").value("DELETED"));
    }
    
    @Test
    @DisplayName("删除用户 - 用户不存在")
    void deleteUser_UserNotFound() throws Exception {
        // When
        ResultActions result = performDelete("/api/users/{userId}", 99999L);
        
        // Then
        result.andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.errMsg").value(org.hamcrest.Matchers.containsString("用户不存在")));
    }
    
    @Override
    protected void initTestData() {
        // 清理测试数据
        truncateTable("t_user");
    }
    
    @Override
    protected void clearTestData() {
        // 清理测试数据
        truncateTable("t_user");
    }
}
