#set($hash = '#')
${hash} ${artifactId}

${hash}${hash} 项目结构说明
```
${artifactId}
├── api/                  # 系统对外暴露服务的声明
├── application/          # 应用层，处理业务逻辑
├── domain/               # 领域层，封装核心业务逻辑
├── infra/                # 基础设施层，数据库与外部集成
│   ├── external/         # 外部接口实现模块
│   ├── messaging/        # 消息队列处理模块
│   ├── persistence/      # 数据库持久化模块
│   ├── rest/             # RESTful 接口模块
│   └── rpc/              # RPC 接口模块
├── shared/               # 共享层，工具与通用组件
└── start/                # 启动模块
```

${hash}${hash} 系统架构
```
${artifactId}-api         系统对外暴露服务的声明
      |
${artifactId}-application  业务编排层
      |
${artifactId}-domain       核心领域层
      |
+-------------------+
|                   |
${artifactId}-infra <-- ${artifactId}-shared (公共工具模块)
      |
+--------------+---------+---------+---------+---------+
| external     | messaging | persistence | rest    | rpc |
+--------------+---------+---------+---------+---------+
      |
${artifactId}-start       系统启动模块
```