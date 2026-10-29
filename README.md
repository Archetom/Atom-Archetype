# Atom Archetype

[![Maven Central](https://img.shields.io/maven-central/v/io.github.archetom/atom-archetype.svg)](https://central.sonatype.com/artifact/io.github.archetom/atom-archetype)
[![CI](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml/badge.svg)](https://github.com/Archetom/atom-archetype/actions/workflows/ci.yml)
[![Java 27](https://img.shields.io/badge/Java-27-007396.svg)](https://openjdk.org/projects/jdk/27/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

简体中文 | [English](README.en.md)

Atom Archetype 是一个 Maven Archetype，用于生成采用领域驱动设计（DDD）的多模块项目。当前 `main` 基于 Java 27 和 Spring Boot 4.1。

生成结果是标准 Maven 工程。领域、应用和基础设施模块之间的依赖边界已经配置好，示例代码可以直接修改或删除。

当前 `main` 还提供面向 AI 辅助开发的 Agent Skills、增量验证和独立业务任务 Benchmark，复用现有架构与测试工具，不增加应用运行时依赖。

## 版本说明

- `main` 是未发布的开发版本，Archetype 开发和生成项目均要求 JDK 27、Maven 3.10.0。Java 27 是非 LTS 版本，工具链升级说明见[升级指南](docs/upgrade-guide.md)。
- `2.2.0` 是当前稳定版，使用 JDK 25。它包含不兼容变更，从 `2.1.0` 升级前请先阅读[升级指南](docs/upgrade-guide.md)。
- [`v2.0.0`](https://github.com/Archetom/Atom-Archetype/tree/v2.0.0) 是 Java 21 版本的旧标签。
- Maven Central `1.1.0` 属于 Spring Boot 3.5 旧架构。

以下快速开始直接使用 Maven Central 上的 `2.2.0`。

## 快速开始

稳定版 `2.2.0` 需要 JDK 25、Docker 和 Docker Compose v2。生成后使用项目附带的 Maven Wrapper；生成命令使用 Maven 3.9.16 或更高版本。

### 1. 生成项目

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=2.2.0 \
  -DgroupId=com.example.orders \
  -DartifactId=orders-service \
  -Dpackage=com.example.orders \
  -Dversion=1.0.0-SNAPSHOT
```

### 2. 构建并运行

```bash
cd orders-service
docker compose up -d mysql
sh ./mvnw clean install

ATOM_SECURITY_TRUSTED_HEADER_ENABLED=true \
  sh ./mvnw -pl start -am spring-boot:run -Dspring-boot.run.profiles=dev
```

应用启动后可检查健康端点：

```bash
curl http://localhost:8080/actuator/health
```

开发身份请求头、Redis 和生产配置见[快速上手](docs/getting-started.md)。

## 生成内容

- `domain`、`application`、`api` 与基础设施模块之间的依赖边界。
- 显式的 `AuthenticatedCaller` 和 `TenantId`，仓储与缓存按租户访问。
- MyBatis-Plus、Flyway 和 MySQL。
- Spring Security、SpringDoc OpenAPI 和统一的 HTTP 错误映射。
- 默认关闭的 Redis 缓存适配器，以及对应的空实现。
- 默认关闭的 gRPC 服务端（Spring Boot 内置 gRPC 支持），使用 Bearer JWT 认证，并复用与 HTTP 相同的 Facade。
- 独立的命令事务与只读查询快照事务、事务提交后回调和 Testcontainers 集成测试。
- 具名分页响应、稳定并发错误，以及会自动编译和测试生成项目的 Archetype 回归测试。

具体依赖版本以生成项目的 `pom.xml` 和 `docker-compose.yml` 为准。

业务 API 默认需要认证。生产环境配置 JWT 签发方后，HTTP 与 gRPC 都使用 Bearer JWT 认证；开发身份请求头仅适用于显式启用的 `dev`、`test` 环境。

## AI 辅助开发

以下能力属于尚未发布的 `main`，不包含在上方快速开始使用的 `2.2.0` 中。可按[开发](#开发)中的步骤从当前源码生成项目。

### Agent Skills

生成项目通过 `AGENTS.md` 约定架构、安全和验证规则，通过 `llms.txt` 索引文档，并在 `.agents/skills/` 提供五个按需读取的 Markdown 工作流程：

| Skill | 用途 |
|---|---|
| [create-feature](src/main/resources/archetype-resources/.agents/skills/create-feature/SKILL.md) | 从领域行为到应用用例及所需适配器，实现业务功能 |
| [add-api](src/main/resources/archetype-resources/.agents/skills/add-api/SKILL.md) | 新增或修改 HTTP API，覆盖契约、权限、转换和回归测试 |
| [add-migration](src/main/resources/archetype-resources/.agents/skills/add-migration/SKILL.md) | 同步演进 Flyway、持久化映射和领域恢复，检查租户与乐观锁行为 |
| [review-architecture](src/main/resources/archetype-resources/.agents/skills/review-architecture/SKILL.md) | 审查 DDD 边界、安全、持久化与事务问题 |
| [verify-change](src/main/resources/archetype-resources/.agents/skills/verify-change/SKILL.md) | 选择验证范围，执行检查并报告未验证的行为 |

这些工作流程不绑定特定模型或编辑器，清理 User 示例后仍保留。ArchUnit 架构测试和现有单元、集成测试负责验证相应约束。详见[AI 工程工作流](docs/ai-engineering.md)。

### 增量验证与结构化报告

在仓库根目录或从 `main` 生成的项目根目录执行：

```bash
make agent-check SCOPE=affected  # 根据 Git 变更保守选择验证范围
make agent-check SCOPE=fast      # 显式运行不依赖 Docker 的检查
make agent-check                # 默认 full，包含 Docker 集成测试
```

`affected` 检查已暂存、未暂存、未跟踪以及删除、移动的文件。生成项目仅对可识别的文档改动，或单个 `domain` / `application` 模块的普通 Java 测试改动缩小范围；仓库中的模板文档改动仍需生成验证。生产代码、POM、跨模块及安全等敏感改动，以及无法可靠判断的变更，均回退到完整验证。该选择器需要 Python 3，缺失时同样回退到 `full`。

原有 `docs`、`generate`（仅仓库）、`fast`、`full` 范围继续可用。标准输出为 JSON，记录实际范围、耗时、退出码和已生成的测试报告路径；详细日志写入标准错误。缺少必需的 Docker 环境会报告阻塞并返回非零退出码，`fast` 通过不代表集成测试通过。各范围的检查内容见[验证指南](docs/ai-engineering.md#verification-entry-point)。

### 业务开发 Benchmark

仓库中的 `benchmarks/` 提供离线评测工具，对固定代码提交生成的项目开展三类任务，每次使用独立工作目录，并在提交后运行独立验收测试：

| 场景 | 任务与验收重点 |
|---|---|
| [add-api](benchmarks/scenarios/add-api/task.md) | 修改用户邮箱：输入校验、认证、权限、租户隔离与持久化结果 |
| [domain-invariant](benchmarks/scenarios/domain-invariant/task.md) | 锁定用户经复核后才能激活：聚合状态转换、非法操作与原有行为 |
| [persistence-change](benchmarks/scenarios/persistence-change/task.md) | 新增首选语言：Flyway 升级、映射与恢复、查询输出、租户隔离和乐观锁 |

评测不自动调用模型 API，可手工接入所选 Coding Agent；结果输出为 JSON 和 Markdown，记录任务成功、首次提交成功、架构与安全违规、关键行为验收覆盖、耗时等指标。无法获取的 Token、人工干预等数据标记为 `unavailable`。编译通过不足以判定任务成功。

`baseline` 与 `engineering` 两组均保留 `AGENTS.md`，对照衡量的是额外 Skills 的增量作用；比较时须保持代码版本、模型版本和执行环境一致。评测器校准结果不代表模型能力或效率提升。普通 CI 只运行确定性测试，真实 Agent 评测按需执行；Benchmark 不进入生成项目的生产依赖。准备、提交、评测和对照命令见[Benchmark 使用说明](benchmarks/README.md)。

## 项目结构

| 模块 | 职责 |
|---|---|
| `api` | 对外请求、响应和 Facade 契约（HTTP 与 RPC 共用，不含身份参数） |
| `domain` | 聚合、值对象、领域事件、仓储与领域服务端口 |
| `shared` | 框架中立的结果与错误类型 |
| `application` | 用例编排、命令/查询模板、事务回调和输出端口 |
| `infra/rest` | Spring MVC、Spring Security、OpenAPI 和错误映射 |
| `infra/persistence` | MyBatis-Plus、Flyway 和缓存适配器 |
| `infra/external` | 第三方系统适配器 |
| `infra/security` | 密码哈希等安全适配器 |
| `infra/facade` | Facade 契约实现 |
| `infra/grpc` | 可选的 gRPC 服务端（默认关闭）：proto 契约、JWT 认证和状态码映射 |
| `start` | Spring Boot 启动入口和运行时装配 |

`domain` 不依赖 `application`、`api`、`shared` 或任何 `infra` 模块。完整规则见[架构设计](docs/architecture.md)。

## 文档

- [快速上手](docs/getting-started.md)
- [架构与依赖规则](docs/architecture.md)
- [命名规范](docs/naming-conventions.md)
- [AI 工程工作流与验证范围](docs/ai-engineering.md)
- [Agent Benchmark 使用说明](benchmarks/README.md)
- [升级指南](docs/upgrade-guide.md)
- [故障排查](docs/troubleshooting.md)
- [变更记录](CHANGELOG.md)

## 开发

开发 `main` 需要 JDK 27；仓库及其生成项目使用 Maven Wrapper 3.10.0，系统 Maven 的最低版本也是 3.10.0。模板位于 `src/main/resources/archetype-resources/`，元数据位于 `src/main/resources/META-INF/maven/archetype-metadata.xml`。

```bash
CI=false make demo
cd target/generated-projects/atom-demo
sh ./mvnw compile
CI=true sh ./mvnw test   # 需要 Docker
```

修改模板后需要同时验证 Archetype 生成过程和生成后的 Maven Reactor。

## 贡献

提交改动前请阅读[贡献指南](CONTRIBUTING.md)和[安全策略](SECURITY.md)。Issue 和 Pull Request 可以直接提交到本仓库。

## 许可证

[MIT](LICENSE)
