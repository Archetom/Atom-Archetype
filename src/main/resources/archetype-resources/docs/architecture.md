# 系统架构说明

## 1. 架构目标

本脚手架旨在为**现代中大型 Java/SpringBoot 项目**提供清晰、分层、可扩展、便于协作与演进的工程基础。

采用经典 DDD（领域驱动设计）+ 分层架构思想，明确各层边界，降低模块间耦合，提高可维护性与测试友好性。

---

## 2. 项目分层结构

```text
${artifactId}
├── api/                  # API接口声明层
├── application/          # 应用层，编排业务逻辑、服务模板、DTO/VO等
├── domain/               # 领域层，核心业务模型与规则
├── infra/                # 基础设施层，第三方集成、数据存储、消息等
│   ├── external/         # 外部服务集成
│   ├── messaging/        # 消息系统集成
│   ├── persistence/      # 持久化实现
│   ├── rest/             # REST接口实现
│   └── rpc/              # RPC接口实现
├── shared/               # 通用基础组件、工具、异常体系等
└── start/                # 启动模块（main入口）
```

---

## 3. 各层职责说明

- **api**
    - 声明对外暴露的DTO、请求/响应对象、接口定义（如 facade）
    - 无任何业务逻辑实现，便于接口与实现解耦
- **application**
    - 负责业务编排和协调，处理参数校验、DTO/VO装配
    - 封装 ServiceTemplate、责任链模板、服务入口等
- **domain**
    - 核心业务模型、实体、领域服务、业务规则
    - 不依赖外部技术（MyBatis、Spring等），强调纯业务能力
- **infra**
    - 与外部系统对接的具体实现，如数据库、MQ、三方API等
    - 基础设施适配、技术细节封装
- **shared**
    - 工具类、常量、异常、通用模板、基础通用组件
- **start**
    - 应用主入口，集中所有模块依赖，独立启动

---

## 4. 技术选型与架构亮点

- **Spring Boot**：主流微服务开发框架，约定优于配置
- **MyBatis-Plus**：简化ORM开发，支持代码生成
- **责任链+模板方法模式**：ServiceTemplate统一业务处理流程
- **分层DTO/VO/PO/Entity对象映射**：推荐MapStruct或手工映射
- **统一异常处理、错误码规范**：全局异常Advice，Result包装
- **线程池/WebClient配置**：支持yml参数化，扩展灵活
- **租户（tenant）与审计字段**：支持多租户与审计自动填充

---

## 5. 典型调用流程

```text
1. Client/API
   ↓
2. api 层
   ↓
3. application 层（参数校验 → 业务编排 → 责任链/模板）
   ↓
4. domain 层（核心业务处理）
   ↓
5. infra 层（数据持久化、消息发送、外部系统调用）
   ↓
6. application 层（结果组装、DTO转换）
   ↓
7. api 层（响应组装、返回前端/客户端）
   ```

---

## 6. 依赖方向与耦合原则

- api/application 只依赖 domain/shared，不直接依赖 infra
- domain **不得**依赖 infra，做到领域独立
- infra 依赖 domain（如持久化层 PO/DO ←→ 领域对象），但不反向依赖
- shared 独立于所有层，可被任意层引用

---

## 7. 扩展点说明

- **新业务扩展**：优先在 application/domain 层实现，infra 仅做外部适配
- **新对象映射**：统一使用 assembler/convertor 实现 DTO-VO-PO-Entity 映射
- **ServiceTemplate**：建议业务调用全部用模板方式接入，便于标准化异常与日志
- **持久化和三方系统**：推荐通过 Repository/Dao + 外部接口适配器解耦

---

## 8. 推荐开发/协作流程

1. **新建业务接口**：api 层声明 DTO/Facade，application 层编写 Service/Template，domain 层建模
2. **补充/完善文档**：所有新增公共抽象需补文档说明
3. **单元/集成测试**：domain/application/infra 层均需配套测试

---

## 9. 架构图示意

```
[前端/客户端]
      ↓
   [API层]
      ↓
   [应用层]
      ↓
   [领域层]
      ↓
  [基础设施层]
      ↓
[DB/消息/外部系统]
```

---

## 10. 文档链接索引

- [开发指南](./usage-guide.md)
- [配置说明](./configuration.md)
- [对象分层说明](./object-layering.md)
- [测试指南](./test-guide.md)

---

如有新模块/新规范，欢迎补充完善！