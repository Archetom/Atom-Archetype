# Atom Archetype — 面向 Spring Boot 4 与 Java 21 的 DDD Maven Archetype

[![Maven Central 旧版](https://img.shields.io/maven-central/v/io.github.archetom/atom-archetype.svg?label=Maven%20Central%20legacy)](https://central.sonatype.com/artifact/io.github.archetom/atom-archetype)
[![CI](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml/badge.svg)](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-007396.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](README.md) | 简体中文

Atom Archetype 是一个用于生成多模块 Java 应用的 Maven Archetype，面向领域驱动设计（DDD）、Spring Boot 4 和 Java 21。它提供明确的依赖边界、显式的调用者与租户上下文、MyBatis-Plus 持久化、Flyway 数据库迁移、安全的 HTTP 默认行为，以及可选的 Redis 缓存。

生成后的项目是团队完全拥有的普通 Maven 工程，不依赖隐藏的运行时框架，也不会把领域模型锁定在生成器中。

## 为什么选择 Atom Archetype

- **清晰的 DDD 边界：** 领域模块保持框架中立；应用层承载用例和输出端口；基础设施通过适配器实现端口。
- **默认安全：** 业务 API 拒绝匿名访问；受信身份 Header 只能在显式启用的 `dev`、`test` 环境使用。
- **租户边界显式：** 已认证调用者和租户 ID 是用例契约的一部分，仓储与缓存均按租户隔离。
- **可靠的持久化基线：** MyBatis-Plus、唯一的 Flyway schema 来源、乐观锁和 MySQL Testcontainers 测试。
- **事务感知的副作用：** 缓存更新和进程内领域事件在数据库事务成功提交后执行。
- **基础设施可选：** Redis 默认关闭，并由 no-op 缓存适配器替代；应用正确性和启动不依赖 Redis。
- **代码可读、可替换：** 生成的是标准 Maven Reactor，团队可以自由裁剪或替换任何实现。

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9 或更高版本
- Docker 与 Docker Compose v2，用于本地 MySQL 和集成测试

### 发布状态

当前稳定版本是 **2.0.0**。Maven Central 上的 `1.1.0` 是 Spring Boot 3.5 旧架构，不包含本文描述的安全、租户、Flyway 与命令/查询模板改造。

### 1. 从 Maven Central 生成项目

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.0.0 \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

### 2. 启动 MySQL 并构建

```bash
cd orders-service
docker compose up -d mysql
sh ./mvnw clean install
```

应用启动时由 Flyway 创建并校验数据库结构。Redis 是可选能力，未开启 Redis 功能时无需启动。

### 3. 显式使用开发环境启动

```bash
SPRING_PROFILES_ACTIVE=dev \
ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
sh ./mvnw -f start/pom.xml spring-boot:run
```

验证公开健康检查：

```bash
curl http://localhost:8080/actuator/health
```

使用仅限开发环境的身份 Header 调用受保护接口：

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H 'Content-Type: application/json' \
  -H 'X-Dev-User-Id: 1001' \
  -H 'X-Dev-Tenant-Id: 42' \
  -d '{
    "username": "alice_01",
    "email": "alice@example.com",
    "password": "correct-horse-battery-staple",
    "realName": "Alice"
  }'
```

Redis、测试、生产配置和身份系统接入说明见 [快速上手](docs/getting-started.md)。

## 生成后的架构

```text
REST / Facade 适配器
          │
          ▼
      应用用例 ─────────────► 输出端口
          │                     ▲
          ▼                     │ 由适配器实现
    框架中立的领域层       持久化 / 外部系统适配器

                 start = 组合根
```

| 模块 | 职责 |
|---|---|
| `api` | 对外请求/响应、Facade 接口、已认证调用者上下文 |
| `domain` | 聚合、值对象、领域事件、策略、仓储和领域服务端口 |
| `shared` | 框架中立的结果与错误约定 |
| `application` | 用例编排、命令/查询模板、事务钩子、输出端口 |
| `infra/rest` | Spring MVC、Spring Security、OpenAPI、HTTP 错误映射 |
| `infra/persistence` | MyBatis-Plus、Flyway、可选 Redis 适配器 |
| `infra/external` | 实现应用层输出端口的第三方系统适配器 |
| `infra/security` | 密码哈希等安全技术适配器 |
| `infra/facade` | 已发布 Facade 契约的实现 |
| `start` | Spring Boot 启动入口与运行时装配 |

核心规则是：**领域层永远不依赖基础设施层**。完整依赖和事务模型见 [架构设计](docs/architecture.md)。

## 安全默认行为

- `/api/**` 必须认证；用户接口还分别要求 `users:read`、`users:write` 或 `users:delete`。
- `/actuator/health` 允许匿名访问。OpenAPI/Swagger 端点启用时允许匿名访问，但生产环境默认关闭。
- 只有在 `dev` 或 `test` profile 生效且显式开启受信 Header 时，才接收 `X-Dev-User-Id` 与 `X-Dev-Tenant-Id`。
- 生产配置禁止受信 Header。生产系统应通过 Spring Security 接入自己的 IdP，并把已验证 principal 映射为 `AuthenticatedCaller`。
- 生产数据库 URL、用户名和密码没有不安全默认值。
- Redis 默认关闭，开启 Redis 是显式的运维决策。

这些默认值建立了安全边界，但示例权限和领域策略仍需按真实产品需求调整。

## 兼容性

| 组件 | 2.0 基线 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.x |
| Maven | 推荐 3.9+ |
| MySQL | Docker Compose 使用 8.4.10；其他 MySQL 8 部署需自行验证 |
| Redis | 可选，7.4.9 |
| MyBatis-Plus | 3.5.16 |
| SpringDoc OpenAPI | 3.0.3 |

当前验证目标是 JVM 部署。GraalVM native-image 尚未进入持续兼容测试，因此模板不会默认生成相关配置。

当前生成命令固定使用精确的 `2.0.0` 正式版本，以保证生成过程可复现。Archetype 升级不会自动改写已经生成的项目，请参考 [升级指南](docs/upgrade-guide.md)。

## 文档入口

- [快速上手](docs/getting-started.md)
- [架构与依赖规则](docs/architecture.md)
- [命名规范](docs/naming-conventions.md)
- [升级指南](docs/upgrade-guide.md)
- [发布检查清单](docs/releasing.md)
- [故障排查](docs/troubleshooting.md)
- [变更记录](CHANGELOG.md)
- [贡献指南](CONTRIBUTING.md)
- [安全策略](SECURITY.md)
- [AI/LLM 项目索引](llms.txt)

## 维护 Archetype

模板位于 `src/main/resources/archetype-resources/`，Archetype 元数据位于 `src/main/resources/META-INF/maven/archetype-metadata.xml`。

```bash
make install
make demo
cd ~/Downloads/atom-demo
sh ./mvnw compile
CI=true sh ./mvnw test   # 需要 Docker
```

每次修改模板后，都要同时验证 Archetype 生成过程和生成后的 Maven Reactor。修改 Velocity 模板前请先阅读 [AGENTS.md](AGENTS.md)。

欢迎在 [GitHub 仓库](https://github.com/Archetom/atom-archetype) 提交 Issue 和 Pull Request。

## 许可证

[MIT](LICENSE)
