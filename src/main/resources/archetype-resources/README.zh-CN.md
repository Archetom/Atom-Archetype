#set( $h2 = '##' )
# ${rootArtifactId}

[English](README.md)

这是一个面向生产实践的 Java 21、Spring Boot 4.1 多模块项目骨架，采用领域驱动设计（DDD）与端口/适配器边界，内置显式多租户、MyBatis-Plus、MySQL、Flyway，以及可选的 Redis 缓存。

生成坐标：`${groupId}:${rootArtifactId}:${version}`。

生成项目中的用户聚合是一套可运行示例。替换或扩展示例时，请继续遵守本文档中的依赖、安全与数据一致性规则。

$h2 核心特性

- 领域逻辑不依赖 HTTP、MyBatis、Redis 等基础设施。
- 所有用户仓储和缓存操作都必须显式传入 `TenantId`。
- API 边界将可信身份转换为 `AuthenticatedCaller`，身份不从请求体获取。
- 写操作和读操作分别使用 `CommandServiceTemplate`、`QueryServiceTemplate`。
- 数据库结构只通过追加式 Flyway migration 演进。
- 聚合使用持久化 `version` 字段进行乐观锁控制。
- 缓存更新和领域事件只在事务成功提交后执行。
- Redis 是可选优化；关闭时由 No-Op 适配器保证业务仍可正确运行。
- 生产环境默认拒绝匿名 API 请求，并且无法启用开发可信头适配器。

$h2 环境要求

- JDK 21
- Docker 与 Docker Compose，用于本地 MySQL 和集成测试
- MySQL 9.7.1 LTS 或兼容的 MySQL 9.7 服务
- Make（可选；每个 Make 目标都有对应的 `sh ./mvnw` 命令）

Redis 是可选组件。

复用已有 MySQL 数据卷时，必须先升级到 MySQL 8.4 LTS，再迁移到 9.7 LTS。不要只修改 Compose 镜像标签而跳过受支持的 LTS 升级步骤。

$h2 快速开始

启动 MySQL：

```bash
docker compose up -d mysql
```

构建全部模块：

```bash
sh ./mvnw clean install
```

使用 `dev` profile 启动，并显式开启仅供本地使用的可信头认证：

```bash
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway 会在启动时自动建表。配置文件位于 `conf/`，不在 `start/src/main/resources`。

检查公开健康端点：

```bash
curl http://localhost:8080/actuator/health
```

调用需要认证的开发接口：

```bash
curl \
  -H 'X-Dev-User-Id: 1' \
  -H 'X-Dev-Tenant-Id: 1' \
  http://localhost:8080/api/v1/users
```

开发环境的 API 文档地址为 `http://localhost:8080/swagger-ui/index.html`，OpenAPI JSON 位于 `/v3/api-docs`。生产环境默认关闭这两个端点；只有在配置了合适的访问策略后才应显式开启。

如果没有显式启用可信头，业务 API 返回 HTTP 401 是预期行为。

$h2 可选 Redis 缓存

启动 Redis 并启用适配器：

```bash
docker compose up -d redis

ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
ATOM_REDIS_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

通过 Spring Boot 标准 Redis 环境变量配置地址、端口、凭据和 TLS。Redis 只负责性能优化，不能成为业务事实来源。

$h2 模块职责

| 模块 | 职责 |
| --- | --- |
| `api` | 对外 Request、Response、Facade 契约与 `AuthenticatedCaller` |
| `domain` | 聚合、值对象、领域服务、领域事件与仓储端口 |
| `application` | 用例编排、命令/查询策略、输出端口与事务回调 |
| `shared` | 少量通用结果和错误基础类型，不放业务模型 |
| `infra/rest` | Spring MVC、Spring Security 与 HTTP 错误映射 |
| `infra/persistence` | MyBatis-Plus、Flyway、乐观锁与缓存适配器 |
| `infra/external` | 实现应用层输出端口的第三方系统适配器 |
| `infra/security` | 密码哈希等安全技术适配器 |
| `infra/facade` | 对外 API Facade 契约实现 |
| `start` | Spring Boot 组合根与集成测试 |

核心依赖方向：

```text
HTTP / persistence / external adapters
                  ↓
             application
                  ↓
               domain
```

`domain` 禁止依赖 `application`、`api` 或任何 `infra` 模块。

$h2 常用命令

```bash
make compile           # 编译全部模块
make test              # 快速测试，不启动 Docker 集成测试
make integration-test  # 使用 MySQL Testcontainers 运行完整测试
make infra-up          # 启动本地 MySQL 和 Redis
make run               # 使用 dev profile 启动 start 模块
make clean-sample      # 安全移除内置 User 示例
```

`make clean-sample` 会通过 `bash clean.sh` 执行，因为 Maven archetype 不能跨平台保留脚本执行位。

$h2 安全模型

所有应用用例都接收 `AuthenticatedCaller`。应用层从中创建经过校验的 `TenantId`，并将租户显式传入仓储与缓存。

内置的 `X-Dev-User-Id`、`X-Dev-Tenant-Id` 认证适配器只用于本地开发和测试。只有同时满足以下条件才会安装：

- 激活 `dev` 或 `test` profile；
- 未激活 `prod` profile；
- 显式设置 `atom.security.trusted-header.enabled=true`。

不要通过公网网关暴露该适配器。生产环境应接入 OAuth2 Resource Server 等真实认证机制，并把已验证身份映射为 `AuthenticatedCaller`。

内置的 `LoggingUserNotificationAdapter` 同样只用于非生产环境。启用 `prod` profile 前必须实现真实的 `UserNotificationPort`；缺少实现时应用会快速启动失败，避免把日志模拟误认为通知已送达。

完整配置与密钥策略见[配置指南](docs/configuration.md)。

$h2 数据库与删除语义

- Flyway migration 位于 `infra/persistence/src/main/resources/db/migration`。
- 已执行的 migration 不可修改；数据库变更必须新增版本文件。
- `UserStatus.DELETED` 是唯一软删除表示，不使用隐藏的 MyBatis 逻辑删除列。
- 用户名、邮箱在租户内唯一，并在软删除后继续保留。
- 旧版本聚合更新会返回乐观锁冲突，不会覆盖较新的数据。

$h2 测试

运行快速单元和模块测试：

```bash
sh ./mvnw test
```

运行依赖 Docker/MySQL 的集成测试：

```bash
CI=true sh ./mvnw test
```

集成测试覆盖 Flyway、租户隔离、完整 PO 映射、乐观锁、软删除、HTTP 认证和错误状态码。

当前经过验证的是 JVM 部署；由于尚未纳入测试矩阵，生成项目不会默认启用 GraalVM native-image 支持。

$h2 文档索引

- [架构与不变量](docs/architecture.md)
- [HTTP API 参考](docs/api-reference.md)
- [开发流程](docs/usage-guide.md)
- [配置与安全](docs/configuration.md)
- [对象分层与命名](docs/object-layering.md)
- [测试指南](docs/test-guide.md)
- [AI 协作说明](AGENTS.md)
- [LLM 文档索引](llms.txt)

$h2 许可证

MIT，详见 [LICENSE](LICENSE)。
