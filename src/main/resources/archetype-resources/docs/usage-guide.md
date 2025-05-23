# Usage Guide

本指引帮助开发者基于脚手架进行日常开发、扩展与维护，包含典型流程、示例和最佳实践。

---

## 1. 快速开始

### 1.1 环境准备

- JDK 17+
- Maven 3.6+
- MySQL / 其他必要中间件
- 推荐 IDE: IntelliJ IDEA

### 1.2 启动项目

```bash
# 初始化数据库等依赖
# 修改配置文件 src/main/resources/application.yml
mvn clean install
cd start
mvn spring-boot:run
```

---

## 2. 目录与模块约定

- **api/** 定义所有对外数据结构（DTO、VO、请求、响应、接口声明）。
- **application/** 负责业务编排、DTO组装、服务模板，面向用例。
- **domain/** 核心领域模型、服务、业务规则。
- **infra/** 外部依赖集成（如DB、MQ、三方API），实现仓储和适配。
- **shared/** 通用工具、异常、常量、模板方法、Result封装等。
- **start/** 项目入口，负责组合依赖并启动。

---

## 3. 新增业务接口流程

1. 在 **api/** 层新增 DTO/请求/响应类，定义 facade 接口。
2. 在 **application/** 层创建 Service、实现 ServiceTemplate 回调，编排业务逻辑。
3. 在 **domain/** 层定义核心领域对象、聚合根及领域服务。
4. 在 **infra/** 层实现 repository、DAO、mapper 等存储或三方系统适配。
5. 在 **application/** 层组装出 VO/DTO 供返回。
6. 在 **rest/rpc** 层暴露接口（如 REST Controller、RPC 实现等）。

---

## 4. 对象转换规范

- DTO <-> VO <-> DO/PO <-> Entity 映射推荐统一在 application 层 assembler/convertor 目录下进行。
- 推荐用 MapStruct，也支持手工转换。
- 不同层数据结构分离，禁止 domain 直接依赖 infra/persistence 层对象。

---

## 5. 统一异常与返回规范

- 业务异常建议全部继承 `AppException` 或 `AppUnRetryException`，详细错误码在 `shared/enums/ErrorCodeEnum.java` 定义。
- 所有接口返回统一包裹在 Result（io.github.archetom.common.result.Result）对象中。
- 推荐 Controller 层只负责请求响应转换，业务错误和系统错误通过 RestExceptionAdvice 全局捕获。

---

## 6. 责任链与模板方法模式应用

- 所有 Service 通过 `ServiceTemplate`（如 `AbstractOperatorServiceTemplate`、`AbstractQueryServiceTemplate`）调用，便于标准化日志、异常、审计。
- 核心业务分解为 ServiceCallback 的 checkParam/buildContext/process/persistence/after 等方法，职责清晰。

---

## 7. 持久化开发规范

- 继承 `BaseRepository`（领域）和 `BaseDao`（infra/persistence）进行仓储实现。
- 领域层对象与数据库表字段解耦，持久化对象 PO/DO 不直接暴露给外部。

---

## 8. 配置与扩展建议

- 线程池、WebClient等组件通过 application 层 config+properties 实现，支持 yml 配置及参数动态扩展。
- 三方系统集成建议全部在 infra/external 下封装，便于后续 Mock/替换。

---

## 9. 单元测试与集成测试

- domain/application 层均应有对应测试（见各模块 test/ 目录）。
- 推荐使用 JUnit 5 + Mock 框架。
- 重要功能建议配集成测试，确保各层解耦、功能完整。

---

## 10. 常见问题与解决

- 启动报错请优先检查依赖、配置文件、数据库连接。
- Mapper/DTO 字段不匹配建议使用 @TableField/@JsonProperty 显式指定。
- 领域对象与数据库表结构不同步时，排查 convertor/assembler 是否漏写映射。

---

## 11. 文档链接索引

- [架构设计说明](./architecture.md)
- [对象分层说明](./object-layering.md)
- [配置参考](./configuration.md)
- [测试指南](./test-guide.md)

---

如需进一步扩展或补充本指南，请遵循 docs/contributing.md 约定，持续完善文档与最佳实践。