# 项目名称

本项目由 Atom Archetype 脚手架生成，基于 DDD 分层架构。

```
api/          # 对外服务声明
application/  # 应用层，业务编排
domain/       # 领域层，核心业务
infra/        # 基础设施、数据库、MQ、第三方等
shared/       # 工具与通用组件
start/        # 启动入口
```

1. 修改数据库和相关配置，详见 `docs/configuration.md`
2. 编译并启动：

   ```
   mvn clean install
   cd start
   mvn spring-boot:run
   ```

3. 详细开发指引见 `docs/usage-guide.md`

**文档结构：**

- docs/architecture.md      架构说明
- docs/object-layering.md   领域分层
- docs/configuration.md     配置说明
- docs/usage-guide.md       使用指引
- docs/test-guide.md        测试说明

如需详细说明，请查阅 `docs/` 相关文档。