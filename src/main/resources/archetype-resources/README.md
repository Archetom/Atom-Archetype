#set( $h2 = '##' )
# ${rootArtifactId}

[English](README.en.md)

`${rootArtifactId}` 基于 Java 25 和 Spring Boot 4.1，采用多模块 Maven 结构。项目由 Atom Archetype 生成，并包含一套可运行的用户管理示例。

生成坐标：`${groupId}:${rootArtifactId}:${version}`。如果不需要示例代码，请运行 `make clean-sample`。

$h2 环境要求

- JDK 25
- 内置 Maven Wrapper 3.9.16（若使用系统 Maven，最低版本为 3.9.16）
- Docker 与 Docker Compose，用于本地 MySQL 和集成测试
- Make（可选）

Redis 默认关闭，不是构建或启动项目的必要条件。

$h2 快速开始

启动 MySQL 并构建全部模块：

```bash
docker compose up -d mysql
sh ./mvnw clean install
```

使用 `dev` profile 启动应用：

```bash
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

检查健康端点：

```bash
curl http://localhost:8080/actuator/health
```

配置文件位于 `conf/`。开发环境的 API 文档地址为 `http://localhost:8080/swagger-ui/index.html`。

$h2 常用命令

| 命令 | 用途 |
|---|---|
| `make compile` | 编译全部模块 |
| `make test` | 运行不依赖 Docker 的测试 |
| `make integration-test` | 使用 MySQL Testcontainers 运行完整测试 |
| `make infra-up` | 启动本地 MySQL 和 Redis |
| `make run` | 使用 `dev` profile 启动应用 |
| `make clean-sample` | 移除内置 User 示例 |

没有安装 Make 时，可查看 `Makefile`，直接运行对应的 Maven Wrapper、Docker Compose 或 `clean.sh` 命令。

$h2 项目结构

| 模块 | 职责 |
|---|---|
| `api` | 对外请求、响应、Facade 契约和 `AuthenticatedCaller` |
| `domain` | 聚合、值对象、领域事件、仓储与领域服务端口 |
| `application` | 用例编排、命令/查询模板、输出端口和事务回调 |
| `shared` | 框架中立的结果与错误类型 |
| `infra/rest` | Spring MVC、Spring Security 和 HTTP 错误映射 |
| `infra/persistence` | MyBatis-Plus、Flyway 和缓存适配器 |
| `infra/external` | 第三方系统适配器 |
| `infra/security` | 密码哈希等安全适配器 |
| `infra/facade` | Facade 契约实现 |
| `start` | Spring Boot 组合根和集成测试 |

依赖方向为 `infra → application → domain`。`domain` 不依赖 `application`、`api` 或任何 `infra` 模块，详细规则见[架构文档](docs/architecture.md)。

$h2 配置

- `conf/application-dev.yml`：本地开发配置。
- `conf/application-test.yml`：自动化测试配置。
- `conf/application-prod.yml`：生产配置，不提供数据源凭据默认值。

`X-Dev-User-Id` 和 `X-Dev-Tenant-Id` 请求头仅用于显式启用的 `dev`、`test` 环境。生产环境应接入真实身份系统并映射为 `AuthenticatedCaller`。

Redis 通过 `atom.redis.enabled` 启用，默认使用空缓存实现。Flyway 迁移脚本位于 `infra/persistence/src/main/resources/db/migration`；数据库变更应新增迁移文件，不要修改已执行的迁移。

完整的身份、Redis、数据库和密钥配置见[配置指南](docs/configuration.md)。

$h2 测试

```bash
sh ./mvnw test
CI=true sh ./mvnw test   # 需要 Docker
```

集成测试覆盖 Flyway、租户隔离、乐观锁、软删除、HTTP 认证和错误映射。当前测试目标为 JVM 部署，不包含 GraalVM native-image。

$h2 移除示例代码

项目内置 User 聚合作为分层和测试示例。不需要时可安全移除：

```bash
make clean-sample
```

该命令通过 `bash clean.sh` 删除示例文件，并保留各模块的目录结构。执行后再次运行 `sh ./mvnw test`。

$h2 文档

- [架构与不变量](docs/architecture.md)
- [HTTP API 参考](docs/api-reference.md)
- [开发流程](docs/usage-guide.md)
- [配置与安全](docs/configuration.md)
- [对象分层与命名](docs/object-layering.md)
- [测试指南](docs/test-guide.md)

$h2 许可证

MIT，详见 [LICENSE](LICENSE)。
