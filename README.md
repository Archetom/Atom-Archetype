# Atom Archetype

[![Maven Central 旧版](https://img.shields.io/maven-central/v/io.github.archetom/atom-archetype.svg?label=Maven%20Central%20legacy)](https://central.sonatype.com/artifact/io.github.archetom/atom-archetype)
[![CI](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml/badge.svg)](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml)
[![Java 25](https://img.shields.io/badge/Java-25-007396.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

简体中文 | [English](README.en.md)

Atom Archetype 是一个基于 Java 25 和 Spring Boot 4.1 的 Maven Archetype，用于生成采用领域驱动设计（DDD）的多模块项目。

生成结果是标准 Maven 工程。领域、应用和基础设施模块之间的依赖边界已经配置好，示例代码可以直接修改或删除。

## 版本说明

- `main` 当前版本为 `2.1.0-SNAPSHOT`，开发和生成项目均使用 JDK 25。
- [`v2.0.0`](https://github.com/Archetom/Atom-Archetype/tree/v2.0.0) 是当前分层架构的稳定 Git 标签，编译目标为 Java 21。
- Maven Central 目前只有 `1.1.0`，它属于 Spring Boot 3.5 旧架构。

以下快速开始基于 `main`，需要先将 `2.1.0-SNAPSHOT` 安装到本地 Maven 仓库。

## 快速开始

需要 JDK 25、Docker 和 Docker Compose v2。仓库及生成项目均提供 Maven Wrapper 3.9.16；如果使用系统 Maven，请使用 3.9.16 或更高版本。

### 1. 安装 Archetype

```bash
git clone https://github.com/Archetom/Atom-Archetype.git
cd Atom-Archetype
./mvnw clean install -Dgpg.skip=true
```

### 2. 生成项目

```bash
cd ..
./Atom-Archetype/mvnw -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.1.0-SNAPSHOT \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

### 3. 构建并运行

```bash
cd orders-service
docker compose up -d mysql
sh ./mvnw clean install

ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -f start/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

应用启动后可检查健康端点：

```bash
curl http://localhost:8080/actuator/health
```

开发身份请求头、Redis 和生产配置见[快速上手](docs/getting-started.md)。

## 生成内容

- `domain`、`application`、`api` 与基础设施模块之间的依赖边界。
- 显式的 `AuthenticatedCaller` 和 `TenantId`，仓储与缓存按租户访问。
- MyBatis-Plus 3.5.16、Flyway 和 MySQL 9.7.1 LTS。
- Spring Security、SpringDoc OpenAPI 3.0.3 和统一的 HTTP 错误映射。
- 默认关闭的 Redis 8.8.0 缓存适配器，以及对应的空实现。
- 命令/查询服务模板、事务提交后回调和 Testcontainers 集成测试。

业务 API 默认需要认证。开发身份请求头仅适用于显式启用的 `dev`、`test` 环境；生产环境应接入自己的身份系统。

## 项目结构

| 模块 | 职责 |
|---|---|
| `api` | 对外请求、响应、Facade 契约和调用者上下文 |
| `domain` | 聚合、值对象、领域事件、仓储与领域服务端口 |
| `shared` | 框架中立的结果与错误类型 |
| `application` | 用例编排、命令/查询模板、事务回调和输出端口 |
| `infra/rest` | Spring MVC、Spring Security、OpenAPI 和错误映射 |
| `infra/persistence` | MyBatis-Plus、Flyway 和缓存适配器 |
| `infra/external` | 第三方系统适配器 |
| `infra/security` | 密码哈希等安全适配器 |
| `infra/facade` | Facade 契约实现 |
| `start` | Spring Boot 启动入口和运行时装配 |

`domain` 不依赖 `application`、`api`、`shared` 或任何 `infra` 模块。完整规则见[架构设计](docs/architecture.md)。

## 文档

- [快速上手](docs/getting-started.md)
- [架构与依赖规则](docs/architecture.md)
- [命名规范](docs/naming-conventions.md)
- [升级指南](docs/upgrade-guide.md)
- [故障排查](docs/troubleshooting.md)
- [变更记录](CHANGELOG.md)

## 开发

模板位于 `src/main/resources/archetype-resources/`，元数据位于 `src/main/resources/META-INF/maven/archetype-metadata.xml`。

```bash
make install
make demo
cd ~/Downloads/atom-demo
sh ./mvnw compile
CI=true sh ./mvnw test   # 需要 Docker
```

修改模板后需要同时验证 Archetype 生成过程和生成后的 Maven Reactor。

## 贡献

提交改动前请阅读[贡献指南](CONTRIBUTING.md)和[安全策略](SECURITY.md)。Issue 和 Pull Request 可以直接提交到本仓库。

## 许可证

[MIT](LICENSE)
