# ${artifactId}

> 基于 Atom Archetype 脚手架生成，采用 DDD 分层架构设计

## 📁 项目结构

```
${artifactId}/
├── api/                  # 对外服务声明层
├── application/          # 应用层，业务编排
├── domain/               # 领域层，核心业务
├── infra/                # 基础设施层
│   ├── external/         # 外部服务集成
│   ├── messaging/        # 消息队列
│   ├── persistence/      # 数据持久化
│   ├── rest/             # REST 接口
│   └── rpc/              # RPC 接口
├── shared/               # 共享组件
└── start/                # 启动模块
```
## 🚀 快速开始

### 环境要求

- **JDK**: 21+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+

### 配置和启动

1. **修改配置文件**
   ```bash
   # 编辑数据库配置
   vim start/src/main/resources/application-{env}.yml
   ```

2. **启动项目**
   ```bash
   mvn clean install
   cd start
   mvn spring-boot:run
   ```

3. **验证启动**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## 🛠️ 开发指南

### 新增业务功能

1. **API 层** - 定义请求/响应对象和接口
2. **Application 层** - 实现业务服务和编排逻辑
3. **Domain 层** - 编写核心业务逻辑和实体
4. **Infrastructure 层** - 实现数据访问和外部集成

### 代码生成

```bash
# MyBatis-Plus 代码生成
cd infra/persistence
mvn exec:java -Dexec.mainClass="${package}.infra.persistence.mysql.generator.MyBatisPlusGenerator"
```

### 运行测试

```bash
# 单元测试
mvn test

# 集成测试（需要 Docker）
mvn verify
```

## 📖 详细文档

- 🏗️ [架构设计](docs/architecture.md) - 整体架构和设计理念
- 📋 [开发指南](docs/usage-guide.md) - 日常开发流程和示例
- ⚙️ [配置说明](docs/configuration.md) - 各模块配置参数
- 📊 [对象分层](docs/object-layering.md) - DTO/VO/PO 使用规范
- 🧪 [测试指南](docs/test-guide.md) - 单元测试和集成测试

## 🔧 技术栈

- **框架**: Spring Boot 3.5.0
- **数据库**: MyBatis-Plus + MySQL
- **缓存**: Redis
- **测试**: JUnit 5 + Testcontainers
- **构建**: Maven

## 📝 开发规范

### 分层调用规范

```text
Controller → Service → DomainService → Repository
     ↓         ↓           ↓               ↓
  Request →  DTO/VO  →   Entity      →     PO
```

### 异常处理

```java
// 可重试业务异常
throw new AppException(ErrorCodeEnum.SYSTEM_ERROR, "系统繁忙");

// 不可重试业务异常  
throw new AppUnRetryException(ErrorCodeEnum.PARAM_CHECK_EXP, "参数错误");
```

### 服务模板使用

```java
@Service
public class UserService {
    @Resource(name = "operatorServiceTemplate")
    private ServiceTemplate serviceTemplate;
    
    public Result<UserVO> createUser(UserCreateRequest request) {
        return serviceTemplate.execute(EventEnum.USER_CREATE, new ServiceCallback<UserVO>() {
            @Override
            public UserVO process() {
                // 核心业务逻辑
                return userDomainService.createUser(request);
            }
        });
    }
}
```

## 🤝 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目基于 MIT 许可证开源 - 查看 [LICENSE](LICENSE) 文件了解详情
