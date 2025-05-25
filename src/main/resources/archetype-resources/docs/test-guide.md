# 测试指南

## 测试策略

### 测试金字塔

```text
    /\
   /  \     E2E Tests (少量)
  /____\    
 /      \   Integration Tests (适量)
/________\  Unit Tests (大量)
```

- **单元测试**: 70% - 测试单个类或方法
- **集成测试**: 20% - 测试模块间协作
- **端到端测试**: 10% - 测试完整业务流程

## 单元测试

### 基础配置

继承 `BaseUnitTest` 获得完整的测试工具：

```java
class UserServiceTest extends BaseUnitTest {
    
    @Mock
    private UserDomainService userDomainService;
    
    @Mock
    private ServiceTemplate serviceTemplate;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void should_create_user_successfully() {
        // given
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        
        when(userDomainService.createUser(anyString(), anyString())).thenReturn(mockUser);
        
        // when
        Result<UserVO> result = userService.createUser(request);
        
        // then
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("testuser", result.getData().getUsername());
        verify(userDomainService).createUser("testuser", "test@example.com");
    }
}
```

### 测试工具使用

```java
class UserDomainServiceTest extends BaseUnitTest {
    
    @Test
    void should_throw_exception_when_email_invalid() {
        // 测试异常
        assertAppException(ErrorCodeEnum.PARAM_CHECK_EXP, () -> {
            userDomainService.createUser("test", "invalid-email");
        });
    }
    
    @Test
    void should_validate_user_list() {
        // given
        List<User> users = Arrays.asList(
            createUser("user1"),
            createUser("user2")
        );
        
        // then
        assertListNotEmpty(users);
        assertListSize(users, 2);
    }
    
    @Test
    void should_generate_random_test_data() {
        // 使用基类提供的随机数据生成
        String username = randomString(10);
        Long userId = randomLong(1000);
        LocalDateTime time = randomDateTime();
        
        assertStringNotBlank(username, "username");
        Assertions.assertTrue(userId > 0);
        Assertions.assertNotNull(time);
    }
    
    private User createUser(String username) {
        User user = new User();
        user.setId(randomLong(1000));
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }
}
```

### Mock 最佳实践

```java
class UserRepositoryTest extends BaseUnitTest {
    
    @Mock
    private UserDao userDao;
    
    @InjectMocks
    private UserRepositoryImpl userRepository;
    
    @Test
    void should_save_user_successfully() {
        // given
        User user = new User();
        user.setUsername("testuser");
        
        UserPO savedPO = new UserPO();
        savedPO.setId(1L);
        savedPO.setUsername("testuser");
        
        when(userDao.save(any(UserPO.class))).thenReturn(savedPO);
        
        // when
        User result = userRepository.save(user);
        
        // then
        Assertions.assertEquals(1L, result.getId());
        Assertions.assertEquals("testuser", result.getUsername());
        
        // 验证调用
        ArgumentCaptor<UserPO> captor = ArgumentCaptor.forClass(UserPO.class);
        verify(userDao).save(captor.capture());
        Assertions.assertEquals("testuser", captor.getValue().getUsername());
    }
    
    @Test
    void should_handle_dao_exception() {
        // given
        User user = new User();
        when(userDao.save(any())).thenThrow(new RuntimeException("Database error"));
        
        // when & then
        assertThrowsWithMessage(RuntimeException.class, "Database error", () -> {
            userRepository.save(user);
        });
    }
}
```

## 集成测试

### 基础配置

继承 `BaseIntegrationTest` 获得完整的集成测试环境：

```java
class UserControllerIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_create_user_via_api() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        
        // when & then
        performPost("/api/users", request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }
    
    @Test
    void should_return_validation_error_when_username_blank() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail("test@example.com");
        // username 为空
        
        // when & then
        assertValidationError(
            performPost("/api/users", request)
        );
    }
    
    @Test
    void should_query_users_with_pagination() throws Exception {
        // given - 准备测试数据
        insertTestUser("user1", "user1@example.com");
        insertTestUser("user2", "user2@example.com");
        
        Map<String, String> params = Map.of(
            "page", "1",
            "size", "10",
            "username", "user"
        );
        
        // when & then
        performGetWithParams("/api/users", params)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2));
    }
    
    private void insertTestUser(String username, String email) {
        Map<String, Object> userData = Map.of(
            "username", username,
            "email", email,
            "status", 1,
            "created_time", "2024-01-01 00:00:00"
        );
        insertTestData("user", userData);
    }
}
```

### 数据库测试

```java
class UserRepositoryIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void should_save_and_find_user() {
        // given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ACTIVE);
        
        // when
        User savedUser = userRepository.save(user);
        User foundUser = userRepository.findById(savedUser.getId());
        
        // then
        Assertions.assertNotNull(savedUser.getId());
        Assertions.assertEquals("testuser", foundUser.getUsername());
        Assertions.assertEquals(UserStatus.ACTIVE, foundUser.getStatus());
    }
    
    @Test
    void should_find_users_by_status() {
        // given
        insertTestUser("active1", UserStatus.ACTIVE);
        insertTestUser("active2", UserStatus.ACTIVE);
        insertTestUser("inactive1", UserStatus.INACTIVE);
        
        // when
        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        
        // then
        Assertions.assertEquals(2, activeUsers.size());
        activeUsers.forEach(user -> 
            Assertions.assertEquals(UserStatus.ACTIVE, user.getStatus())
        );
    }
    
    private void insertTestUser(String username, UserStatus status) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setStatus(status);
        userRepository.save(user);
    }
}
```

### 缓存测试

```java
class CacheServiceIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private CacheService cacheService;
    
    @Test
    void should_cache_and_retrieve_object() {
        // given
        String key = "test:user:1";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        
        // when
        cacheService.put(key, user, Duration.ofMinutes(5));
        User cachedUser = cacheService.get(key, User.class);
        
        // then
        Assertions.assertNotNull(cachedUser);
        Assertions.assertEquals(1L, cachedUser.getId());
        Assertions.assertEquals("testuser", cachedUser.getUsername());
    }
    
    @Test
    void should_evict_cache() {
        // given
        String key = "test:user:2";
        setRedisValue(key, "test-value");
        
        // when
        cacheService.evict(key);
        
        // then
        Assertions.assertNull(getRedisValue(key));
    }
}
```

## 性能测试

### 基准测试

```java
class UserServicePerformanceTest extends BaseIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void should_create_users_within_time_limit() {
        // given
        int userCount = 100;
        List<UserCreateRequest> requests = IntStream.range(0, userCount)
            .mapToObj(i -> {
                UserCreateRequest request = new UserCreateRequest();
                request.setUsername("user" + i);
                request.setEmail("user" + i + "@example.com");
                return request;
            })
            .collect(Collectors.toList());
        
        // when
        long startTime = System.currentTimeMillis();
        
        requests.parallelStream().forEach(request -> {
            Result<UserVO> result = userService.createUser(request);
            Assertions.assertTrue(result.isSuccess());
        });
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // then
        Assertions.assertTrue(duration < 5000, 
            "Creating " + userCount + " users took " + duration + "ms, should be less than 5000ms");
    }
}
```

## 测试数据管理

### 测试数据构建器

```java
public class UserTestDataBuilder {
    private String username = "defaultUser";
    private String email = "default@example.com";
    private UserStatus status = UserStatus.ACTIVE;
    private LocalDateTime createdTime = LocalDateTime.now();
    
    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }
    
    public UserTestDataBuilder withUsername(String username) {
        this.username = username;
        return this;
    }
    
    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public UserTestDataBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }
    
    public UserTestDataBuilder inactive() {
        this.status = UserStatus.INACTIVE;
        return this;
    }
    
    public User build() {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setStatus(status);
        user.setCreatedTime(createdTime);
        return user;
    }
    
    public UserCreateRequest buildRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(username);
        request.setEmail(email);
        return request;
    }
}

// 使用示例
@Test
void should_create_inactive_user() {
    // given
    User user = UserTestDataBuilder.aUser()
        .withUsername("testuser")
        .withEmail("test@example.com")
        .inactive()
        .build();
    
    // when & then
    Assertions.assertEquals(UserStatus.INACTIVE, user.getStatus());
}
```

### 测试数据清理

```java
@TestMethodOrder(OrderAnnotation.class)
class UserServiceOrderedTest extends BaseIntegrationTest {
    
    @Test
    @Order(1)
    void should_create_user() {
        // 创建测试数据
    }
    
    @Test
    @Order(2)
    void should_update_user() {
        // 使用之前创建的数据
    }
    
    @AfterEach
    void cleanupTestData() {
        // 清理特定测试数据
        truncateTable("user");
        clearRedisData();
    }
}
```

## 测试配置

### 测试专用配置

```yaml
# application-test.yml
spring:
  datasource:
    # 使用 Testcontainers 自动配置
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    
logging:
  level:
    org.springframework.web: DEBUG
    com.example: DEBUG
    org.testcontainers: INFO
    
# 测试专用配置
app:
  test:
    data-cleanup: true
    mock-external-services: true
```

### 测试 Profile

```java
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.feature.enabled=false",
    "spring.cache.type=none"
})
class DisabledFeatureTest extends BaseIntegrationTest {
    // 测试功能禁用场景
}
```

## 常见测试场景

### 异步操作测试

```java
@Test
void should_handle_async_operation() throws Exception {
    // given
    CompletableFuture<String> future = asyncService.processAsync("test");
    
    // when
    String result = future.get(5, TimeUnit.SECONDS);
    
    // then
    Assertions.assertEquals("processed: test", result);
}
```

### 事务测试

```java
@Test
@Transactional
@Rollback(false)  // 不回滚，验证事务提交
void should_commit_transaction() {
    // 测试事务提交场景
}

@Test
void should_rollback_on_exception() {
    // 测试事务回滚场景
    assertThrows(RuntimeException.class, () -> {
        userService.createUserWithError();
    });
    
    // 验证数据未保存
    List<User> users = userRepository.findAll();
    Assertions.assertTrue(users.isEmpty());
}
```

### 安全测试

```java
@Test
@WithMockUser(roles = "ADMIN")
void should_allow_admin_access() throws Exception {
    performGet("/api/admin/users")
        .andExpect(status().isOk());
}

@Test
void should_deny_unauthorized_access() throws Exception {
    performGet("/api/admin/users")
        .andExpect(status().isUnauthorized());
}
```

## 测试最佳实践

### 命名规范

```java
// ✅ 好的测试方法命名
@Test
void should_create_user_when_valid_request_provided() { }

@Test
void should_throw_exception_when_username_already_exists() { }

@Test
void should_return_empty_list_when_no_users_found() { }

// ❌ 不好的命名
@Test
void testCreateUser() { }

@Test
void test1() { }
```

### 测试结构

```java
@Test
void should_create_user_successfully() {
    // given - 准备测试数据
    UserCreateRequest request = new UserCreateRequest();
    request.setUsername("testuser");
    
    // when - 执行被测试的操作
    Result<UserVO> result = userService.createUser(request);
    
    // then - 验证结果
    Assertions.assertTrue(result.isSuccess());
    Assertions.assertEquals("testuser", result.getData().getUsername());
}
```

### 断言技巧

```java
@Test
void should_validate_user_properties() {
    User user = userService.getUser(1L);
    
    // 使用 AssertJ 进行流畅断言
    assertThat(user)
        .isNotNull()
        .extracting(User::getUsername, User::getEmail, User::getStatus)
        .containsExactly("testuser", "test@example.com", UserStatus.ACTIVE);
    
    // 验证集合
    List<User> users = userService.getAllUsers();
    assertThat(users)
        .hasSize(3)
        .extracting(User::getUsername)
        .containsExactlyInAnyOrder("user1", "user2", "user3");
}
```

## 持续集成

### Maven 配置

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 测试覆盖率要求

- **单元测试覆盖率**: ≥ 80%
- **集成测试覆盖率**: ≥ 60%
- **关键业务逻辑**: 100%

### CI/CD 集成

```yaml
# .github/workflows/test.yml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests
        run: mvn clean test
      - name: Generate test report
        run: mvn jacoco:report
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```
