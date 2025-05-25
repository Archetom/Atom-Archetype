# 开发指南

## 环境准备

### 系统要求

- **JDK**: 17+
- **Maven**: 3.9+
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+
- **IDE**: IntelliJ IDEA（推荐）

### 项目初始化

```bash
# 1. 生成项目
mvn archetype:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=1.0.1 \
  -DgroupId=com.example \
  -DartifactId=my-project \
  -Dpackage=com.example.myproject

# 2. 进入项目目录
cd my-project

# 3. 修改配置文件
vim start/src/main/resources/application.yml

# 4. 启动项目
mvn clean install
cd start && mvn spring-boot:run
```

## 开发流程

### 完整示例：用户管理功能

#### 1. 定义 API 接口

```java
// api/dto/request/UserCreateRequest.java
@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}

// api/dto/response/UserResponse.java
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdTime;
}

// api/facade/UserFacade.java
public interface UserFacade {
    Result<UserResponse> createUser(UserCreateRequest request);
    Result<UserResponse> getUserById(Long userId);
}
```

#### 2. 实现应用服务

```java
// application/service/UserService.java
@Service
public class UserService {
    
    @Resource(name = "operatorServiceTemplate")
    private ServiceTemplate serviceTemplate;
    
    @Autowired
    private UserDomainService userDomainService;
    
    public Result<UserVO> createUser(UserCreateRequest request) {
        return serviceTemplate.execute(EventEnum.USER_CREATE, new ServiceCallback<UserVO>() {
            @Override
            public void checkParam() {
                if (StringUtils.isBlank(request.getUsername())) {
                    throw new AppUnRetryException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户名不能为空");
                }
            }
            
            @Override
            public UserVO process() {
                User user = userDomainService.createUser(request.getUsername(), request.getEmail());
                return UserAssembler.toVO(user);
            }
        });
    }
}
```

#### 3. 领域层实现

```java
// domain/entity/User.java
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdTime;
    
    public void validateEmail() {
        if (!email.contains("@")) {
            throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "邮箱格式不正确");
        }
    }
}

// domain/service/UserDomainService.java
public interface UserDomainService {
    User createUser(String username, String email);
    User findById(Long userId);
}

// domain/service/impl/UserDomainServiceImpl.java
@Service
public class UserDomainServiceImpl implements UserDomainService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setCreatedTime(LocalDateTime.now());
        
        user.validateEmail();
        
        return userRepository.save(user);
    }
}
```

#### 4. 基础设施实现

```java
// infra/persistence/repository/UserRepositoryImpl.java
@Repository
public class UserRepositoryImpl implements UserRepository {
    
    @Autowired
    private UserDao userDao;
    
    @Override
    public User save(User user) {
        UserPO userPO = UserConverter.toPO(user);
        userDao.save(userPO);
        return UserConverter.toEntity(userPO);
    }
    
    @Override
    public User findById(Long id) {
        UserPO userPO = userDao.getById(id);
        return userPO != null ? UserConverter.toEntity(userPO) : null;
    }
}

// infra/rest/controller/UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest request) {
        Result<UserVO> result = userService.createUser(request);
        return ResponseEntityUtil.assembleResponse(result);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Result<UserVO> result = userService.getUserById(id);
        return ResponseEntityUtil.assembleResponse(result);
    }
}
```

## 开发规范

### 对象转换

使用 Assembler/Converter 进行对象转换：

```java
// application/assembler/UserAssembler.java
public class UserAssembler {
    public static UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        return vo;
    }
    
    public static UserResponse toResponse(UserVO vo) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(vo, response);
        return response;
    }
}
```

### 异常处理

```java
// 业务异常（可重试）
throw new AppException(ErrorCodeEnum.SYSTEM_ERROR, "系统繁忙，请稍后重试");

// 业务异常（不可重试）
throw new AppUnRetryException(ErrorCodeEnum.PARAM_CHECK_EXP, "参数校验失败");

// 自定义错误码
public enum ErrorCodeEnum {
    USER_NOT_FOUND("404", "USER_NOT_FOUND", "用户不存在", "用户不存在", 
                   ErrorLevelConst.WARN, ErrorTypeConst.BIZ);
}
```

### 分页查询

```java
public Result<Pager<UserVO>> queryUsers(UserQueryRequest request) {
    return serviceTemplate.execute(EventEnum.USER_QUERY, new ServiceCallback<Pager<UserVO>>() {
        @Override
        public Pager<UserVO> process() {
            Page<UserPO> page = userDao.page(
                new Page<>(request.getPage(), request.getSize()),
                new QueryWrapper<UserPO>()
                    .like(StringUtils.isNotBlank(request.getUsername()), "username", request.getUsername())
            );
            
            Pager<UserVO> pager = PageUtil.toPager(page);
            List<UserVO> voList = page.getRecords().stream()
                .map(UserConverter::toEntity)
                .map(UserAssembler::toVO)
                .collect(Collectors.toList());
            pager.setData(voList);
            
            return pager;
        }
    });
}
```

## 常见问题

### Q: 如何添加新的配置项？

A: 在 `application/properties` 包下创建配置类：

```java
@Data
@ConfigurationProperties(prefix = "app.feature")
public class FeatureProperties {
    private boolean enabled = true;
    private String apiUrl;
}
```

### Q: 如何集成新的外部系统？

A: 在 `infra/external` 包下创建适配器：

```java
@Component
public class PaymentServiceAdapter {
    @Autowired
    private WebClient webClient;
    
    public PaymentResult pay(PaymentRequest request) {
        // 调用外部支付接口
    }
}
```

### Q: 如何添加缓存？

A: 使用注入的 `CacheService`：

```java
@Service
public class UserService {
    @Autowired
    private CacheService cacheService;
    
    public User getUserById(Long id) {
        String key = "user:" + id;
        User user = cacheService.get(key, User.class);
        if (user == null) {
            user = userRepository.findById(id);
            cacheService.put(key, user, Duration.ofMinutes(30));
        }
        return user;
    }
}
```

## 代码生成

使用 MyBatis-Plus 代码生成器：

```bash
# 1. 配置数据库连接
vim infra/persistence/src/main/resources/mybatis-plus.yml

# 2. 运行生成器
cd infra/persistence
mvn exec:java -Dexec.mainClass="com.example.infra.persistence.mysql.generator.MyBatisPlusGenerator"

# 3. 按提示输入表名和类名
```

## 测试

### 单元测试

```java
class UserServiceTest extends BaseUnitTest {
    
    @Mock
    private UserDomainService userDomainService;
    
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
    }
}
```

### 集成测试

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
            .andExpect(jsonPath("$.username").value("testuser"));
    }
}
```

## 部署

### 本地部署

```bash
mvn clean package
java -jar start/target/start-1.0.0-SNAPSHOT.jar
```

### Docker 部署

```dockerfile
FROM openjdk:17-jre-slim
COPY start/target/start-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t my-app .
docker run -p 8080:8080 my-app
```

## 最佳实践

### 代码组织

```java
// ✅ 推荐的包结构
com.example.myproject
├── api.dto.request          # API 请求对象
├── api.dto.response         # API 响应对象
├── application.service      # 应用服务
├── application.assembler    # 对象转换器
├── domain.entity            # 领域实体
├── domain.service           # 领域服务
├── infra.persistence        # 数据持久化
└── shared.exception         # 共享异常
```

### 命名规范

```java
// 类命名
UserCreateRequest          # 请求对象
UserResponse               # 响应对象  
UserService                # 应用服务
UserDomainService          # 领域服务
UserRepository             # 仓储接口
UserRepositoryImpl         # 仓储实现
UserPO                     # 持久化对象
UserAssembler              # 对象装配器

// 方法命名
createUser()               # 创建操作
getUserById()              # 查询操作
updateUser()               # 更新操作
deleteUser()               # 删除操作
```

### 错误处理最佳实践

```java
// 1. 统一错误码定义
public enum ErrorCodeEnum {
    USER_NOT_FOUND("001", "USER_NOT_FOUND", "用户不存在", "用户不存在",
                   ErrorLevelConst.WARN, ErrorTypeConst.BIZ),
    
    INVALID_PARAM("002", "INVALID_PARAM", "参数无效", "参数无效", 
                  ErrorLevelConst.WARN, ErrorTypeConst.BIZ);
}

// 2. 业务异常抛出
@Override
public User findById(Long id) {
    if (id == null || id <= 0) {
        throw new AppUnRetryException(ErrorCodeEnum.INVALID_PARAM, "用户ID不能为空或小于等于0");
    }
    
    User user = userRepository.findById(id);
    if (user == null) {
        throw new AppUnRetryException(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在: " + id);
    }
    
    return user;
}

// 3. 全局异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException e) {
        return ResponseEntityUtil.assembleResponse(
            ResultUtil.genErrorResult(new Result<>(), e, "0001", "myapp")
        );
    }
}
```

### 缓存使用最佳实践

```java
@Service
public class UserService {
    
    @Autowired
    private CacheService cacheService;
    
    private static final String USER_CACHE_KEY = "user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    
    public User getUserById(Long id) {
        String cacheKey = USER_CACHE_KEY + id;
        
        // 先查缓存
        User user = cacheService.get(cacheKey, User.class);
        if (user != null) {
            return user;
        }
        
        // 缓存未命中，查数据库
        user = userRepository.findById(id);
        if (user != null) {
            // 写入缓存
            cacheService.put(cacheKey, user, CACHE_TTL);
        }
        
        return user;
    }
    
    public void updateUser(User user) {
        userRepository.save(user);
        
        // 更新后清除缓存
        String cacheKey = USER_CACHE_KEY + user.getId();
        cacheService.evict(cacheKey);
    }
}
```

### 分页查询最佳实践

```java
@Service
public class UserService {
    
    public Result<Pager<UserVO>> queryUsers(UserQueryRequest request) {
        return serviceTemplate.execute(EventEnum.USER_QUERY, new ServiceCallback<Pager<UserVO>>() {
            @Override
            public void checkParam() {
                // 参数校验
                if (request.getPage() < 1) {
                    throw new AppUnRetryException(ErrorCodeEnum.INVALID_PARAM, "页码不能小于1");
                }
                if (request.getSize() > 100) {
                    throw new AppUnRetryException(ErrorCodeEnum.INVALID_PARAM, "每页大小不能超过100");
                }
            }
            
            @Override
            public Pager<UserVO> process() {
                // 构建查询条件
                QueryWrapper<UserPO> wrapper = new QueryWrapper<>();
                wrapper.like(StringUtils.isNotBlank(request.getUsername()), "username", request.getUsername())
                       .eq(request.getStatus() != null, "status", request.getStatus())
                       .orderByDesc("created_time");
                
                // 分页查询
                Page<UserPO> page = new Page<>(request.getPage(), request.getSize());
                Page<UserPO> result = userDao.selectPage(page, wrapper);
                
                // 转换结果
                Pager<UserVO> pager = PageUtil.toPager(result);
                List<UserVO> voList = result.getRecords().stream()
                    .map(UserConverter::toEntity)
                    .map(UserAssembler::toVO)
                    .collect(Collectors.toList());
                pager.setData(voList);
                
                return pager;
            }
        });
    }
}
```

### 事务处理最佳实践

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class UserService {
    
    // 查询操作使用只读事务
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    // 写操作使用默认事务
    public Result<UserVO> createUser(UserCreateRequest request) {
        return serviceTemplate.execute(EventEnum.USER_CREATE, new ServiceCallback<UserVO>() {
            @Override
            public UserVO process() {
                // 业务逻辑处理
                User user = userDomainService.createUser(request.getUsername(), request.getEmail());
                
                // 发送通知（异步，不影响事务）
                applicationEventPublisher.publishEvent(new UserCreatedEvent(user.getId()));
                
                return UserAssembler.toVO(user);
            }
        });
    }
    
    // 需要新事务的操作
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserOperation(Long userId, String operation) {
        // 记录用户操作日志，使用独立事务
        userOperationLogService.log(userId, operation);
    }
}
```

### 性能优化建议

```java
// 1. 批量操作
@Service
public class UserService {
    
    public void batchCreateUsers(List<UserCreateRequest> requests) {
        // 批量转换
        List<User> users = requests.stream()
            .map(request -> {
                User user = new User();
                user.setUsername(request.getUsername());
                user.setEmail(request.getEmail());
                return user;
            })
            .collect(Collectors.toList());
        
        // 批量保存
        userRepository.saveBatch(users);
    }
}

// 2. 异步处理
@Service
public class UserService {
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    public Result<UserVO> createUser(UserCreateRequest request) {
        Result<UserVO> result = // ... 创建用户逻辑
        
        // 异步发送欢迎邮件
        taskExecutor.execute(() -> {
            try {
                emailService.sendWelcomeEmail(result.getData().getEmail());
            } catch (Exception e) {
                log.error("发送欢迎邮件失败", e);
            }
        });
        
        return result;
    }
}

// 3. 数据库查询优化
@Repository
public class UserRepositoryImpl implements UserRepository {
    
    // 使用索引字段查询
    public List<User> findActiveUsers() {
        QueryWrapper<UserPO> wrapper = new QueryWrapper<>();
        wrapper.eq("status", UserStatus.ACTIVE.getCode())
               .orderByDesc("created_time")
               .last("LIMIT 1000"); // 限制查询数量
        
        return userDao.selectList(wrapper).stream()
            .map(UserConverter::toEntity)
            .collect(Collectors.toList());
    }
}
```
