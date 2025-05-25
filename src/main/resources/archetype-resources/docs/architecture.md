# 架构设计

## 设计理念

Atom 脚手架采用 **DDD（领域驱动设计）+ 分层架构**，旨在为中大型 Java 项目提供清晰、可扩展的工程基础。

### 核心原则

- **职责分离** - 每层专注自己的职责，降低耦合
- **依赖倒置** - 高层模块不依赖低层模块
- **开放封闭** - 对扩展开放，对修改封闭

## 分层架构

### 整体架构图

```text
┌─────────────────┐
│    Client/API   │
└─────────┬───────┘
          │
┌─────────▼───────┐
│     API Layer   │  ← 接口声明
└─────────┬───────┘
          │
┌─────────▼───────┐
│   Application   │  ← 业务编排
└─────────┬───────┘
          │
┌─────────▼───────┐
│      Domain     │  ← 核心业务
└─────────┬───────┘
          │
┌─────────▼───────┐
│  Infrastructure │  ← 基础设施
└─────────────────┘
```

### 各层职责

| 层级 | 职责 | 主要组件 |
|------|------|----------|
| **API** | 对外接口声明 | DTO、Facade 接口 |
| **Application** | 业务编排协调 | Service、Assembler、VO |
| **Domain** | 核心业务逻辑 | Entity、Repository、DomainService |
| **Infrastructure** | 技术实现 | Mapper、外部接口、消息队列 |
| **Shared** | 通用组件 | 工具类、异常、常量 |

## 核心特性

### 服务模板模式

所有业务操作通过 `ServiceTemplate` 统一处理：

```java
@Service
public class UserService {
    @Resource(name = "operatorServiceTemplate")
    private ServiceTemplate serviceTemplate;
    
    public Result<UserVO> createUser(UserCreateRequest request) {
        return serviceTemplate.execute(EventEnum.USER_CREATE, new ServiceCallback<UserVO>() {
            @Override
            public void checkParam() {
                // 参数校验
            }
            
            @Override
            public UserVO process() {
                // 核心业务逻辑
                return userDomainService.createUser(request);
            }
        });
    }
}
```

### 责任链处理

内置标准处理步骤：

1. **参数校验** - 检查输入参数
2. **上下文构建** - 准备执行环境
3. **并发控制** - 幂等性检查
4. **业务处理** - 核心逻辑执行
5. **数据持久化** - 保存结果
6. **后置处理** - 清理和通知

### 统一异常处理

```java
// 业务异常
throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户名不能为空");

// 不可重试异常
throw new AppUnRetryException(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
```

## 依赖关系

```text
api ──────────┐
              ▼
application ──┼──► domain ──► shared
              ▼
infra ────────┘
```

**依赖规则**：
- `domain` 不依赖任何其他业务层
- `infra` 可以依赖 `domain`，但 `domain` 不能依赖 `infra`
- `shared` 被所有层依赖，但不依赖任何业务层

## 扩展指南

### 新增业务功能

1. **定义接口** - 在 `api` 层声明 DTO 和 Facade
2. **实现服务** - 在 `application` 层编写 Service
3. **领域建模** - 在 `domain` 层定义实体和业务规则
4. **基础设施** - 在 `infra` 层实现数据访问

### 集成外部系统

1. 在 `domain` 层定义接口
2. 在 `infra/external` 层实现适配器
3. 通过依赖注入使用

## 最佳实践

- ✅ 使用 ServiceTemplate 处理所有业务操作
- ✅ 通过 Assembler/Converter 进行对象转换
- ✅ 在 domain 层编写核心业务逻辑
- ✅ 使用统一的异常和错误码
- ❌ 不要在 Controller 中写业务逻辑
- ❌ 不要让 domain 层依赖 infra 层
- ❌ 不要直接暴露 PO 对象到接口层
