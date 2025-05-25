# 对象分层规范

## 概述

本项目采用严格的对象分层设计，确保各层职责清晰，数据流向明确。

## 对象类型定义

### Request/Response - 接口层对象

**用途**: REST API 的输入输出对象  
**位置**: `api/dto/request/`、`api/dto/response/`  
**特点**: 面向外部接口，包含验证注解

```java
// UserCreateRequest.java
@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}

// UserResponse.java
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String status;
    private LocalDateTime createdTime;
}
```

### DTO - 数据传输对象

**用途**: 服务间、模块间数据传输  
**位置**: `application/dto/`  
**特点**: 纯数据载体，无业务逻辑

```java
// UserDTO.java
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private Integer status;
    private LocalDateTime createdTime;
}
```

### VO - 视图对象

**用途**: 应用层内部数据展示  
**位置**: `application/vo/`  
**特点**: 面向业务展示，可包含计算字段

```java
// UserVO.java
@Data
public class UserVO {
    private Long id;
    private String username;
    private String displayName;    // 计算字段
    private String statusText;     // 状态文本
    private String createdTimeText; // 格式化时间
    
    // 业务方法
    public String getDisplayName() {
        return StringUtils.isNotBlank(nickname) ? nickname : username;
    }
}
```

### Entity - 领域实体

**用途**: 领域层核心业务对象  
**位置**: `domain/entity/`  
**特点**: 包含业务逻辑和规则

```java
// User.java
@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private UserStatus status;
    private LocalDateTime createdTime;
    
    // 业务方法
    public void activate() {
        if (this.status == UserStatus.DELETED) {
            throw new AppException(ErrorCodeEnum.USER_DELETED, "已删除用户无法激活");
        }
        this.status = UserStatus.ACTIVE;
    }
    
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
}
```

### PO - 持久化对象

**用途**: 数据库表映射对象  
**位置**: `infra/persistence/mysql/po/`  
**特点**: 纯数据映射，包含数据库注解

```java
// UserPO.java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class UserPO extends BasePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("username")
    private String username;
    
    @TableField("email")
    private String email;
    
    @TableField("status")
    private Integer status;
}
```

## 数据流转规范

### 典型流转路径

```text
Request → DTO → Entity → PO → Database
   ↓                              ↑
Response ← VO ← Entity ← PO ← Database
```

### 转换示例

```java
// UserAssembler.java - 负责对象转换
public class UserAssembler {
    
    // Request → DTO
    public static UserDTO toDTO(UserCreateRequest request) {
        UserDTO dto = new UserDTO();
        dto.setUsername(request.getUsername());
        dto.setEmail(request.getEmail());
        return dto;
    }
    
    // Entity → VO
    public static UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setStatusText(user.getStatus().getDescription());
        return vo;
    }
    
    // VO → Response
    public static UserResponse toResponse(UserVO vo) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(vo, response);
        return response;
    }
}

// UserConverter.java - Entity ↔ PO 转换
public class UserConverter {
    
    // Entity → PO
    public static UserPO toPO(User user) {
        UserPO po = new UserPO();
        po.setId(user.getId());
        po.setUsername(user.getUsername());
        po.setEmail(user.getEmail());
        po.setStatus(user.getStatus().getCode());
        return po;
    }
    
    // PO → Entity
    public static User toEntity(UserPO po) {
        User user = new User();
        user.setId(po.getId());
        user.setUsername(po.getUsername());
        user.setEmail(po.getEmail());
        user.setStatus(UserStatus.fromCode(po.getStatus()));
        user.setCreatedTime(po.getCreatedTime());
        return user;
    }
}
```

## 分层使用规范

### ✅ 正确使用

```java
// Controller 层
@RestController
public class UserController {
    
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest request) {
        // Request → Service
        Result<UserVO> result = userService.createUser(request);
        
        // VO → Response
        UserResponse response = UserAssembler.toResponse(result.getData());
        return ResponseEntity.ok(response);
    }
}

// Service 层
@Service
public class UserService {
    
    public Result<UserVO> createUser(UserCreateRequest request) {
        return serviceTemplate.execute(EventEnum.USER_CREATE, new ServiceCallback<UserVO>() {
            @Override
            public UserVO process() {
                // Request → Entity
                User user = userDomainService.createUser(request.getUsername(), request.getEmail());
                
                // Entity → VO
                return UserAssembler.toVO(user);
            }
        });
    }
}

// Repository 层
@Repository
public class UserRepositoryImpl implements UserRepository {
    
    @Override
    public User save(User user) {
        // Entity → PO
        UserPO po = UserConverter.toPO(user);
        userDao.save(po);
        
        // PO → Entity
        return UserConverter.toEntity(po);
    }
}
```

### ❌ 错误使用

```java
// ❌ 直接暴露 PO 到 Controller
@GetMapping("/users/{id}")
public UserPO getUser(@PathVariable Long id) {
    return userDao.getById(id);  // 错误：暴露数据库对象
}

// ❌ 在 Domain 层使用 Request/Response
public class UserDomainService {
    public UserResponse createUser(UserCreateRequest request) {  // 错误：领域层不应依赖接口层对象
        // ...
    }
}

// ❌ 混用不同层的对象
@Service
public class UserService {
    public UserPO createUser(UserCreateRequest request) {  // 错误：Service 不应返回 PO
        // ...
    }
}
```

## 对象映射工具

### MapStruct 配置

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    
    @Mapping(target = "statusText", source = "status", qualifiedByName = "statusToText")
    UserVO toVO(User user);
    
    @Named("statusToText")
    default String statusToText(UserStatus status) {
        return status != null ? status.getDescription() : "";
    }
    
    List<UserVO> toVOList(List<User> users);
}
```

### 手动映射最佳实践

```java
public class UserAssembler {
    
    public static UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        
        // 处理枚举转换
        if (user.getStatus() != null) {
            vo.setStatusText(user.getStatus().getDescription());
        }
        
        // 处理时间格式化
        if (user.getCreatedTime() != null) {
            vo.setCreatedTimeText(user.getCreatedTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return vo;
    }
    
    public static List<UserVO> toVOList(List<User> users) {
        return users.stream()
                .map(UserAssembler::toVO)
                .collect(Collectors.toList());
    }
}
```

## 常见问题

### Q: 什么时候使用 DTO，什么时候使用 VO？

A:
- **DTO**: 跨服务、跨模块传输数据时使用
- **VO**: 应用层内部业务处理和数据展示时使用

### Q: 可以跨层使用对象吗？

A: 不建议。每层应该使用自己的对象类型，通过转换器进行映射。

### Q: 如何处理复杂的对象转换？

A:
1. 简单映射：使用 BeanUtils.copyProperties()
2. 复杂映射：使用 MapStruct 或手写 Assembler
3. 业务转换：在 Assembler 中添加业务逻辑

### Q: 分页对象如何处理？

A: 使用泛型分页对象，在不同层进行数据转换：

```java
// 查询 PO 分页
Page<UserPO> poPage = userDao.selectPage(page, wrapper);

// 转换为 Entity 分页
Pager<User> entityPager = PageUtil.toPager(poPage);
List<User> users = poPage.getRecords().stream()
    .map(UserConverter::toEntity)
    .collect(Collectors.toList());
entityPager.setData(users);

// 转换为 VO 分页
Pager<UserVO> voPage = PageUtil.copy(entityPager);
voPage.setData(UserAssembler.toVOList(users));
```
