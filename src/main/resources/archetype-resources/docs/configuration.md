# 配置说明

本指南说明脚手架各模块关键配置项，及其作用与扩展方式。

---

## 1. 配置文件入口

- 全局配置文件位于：  
  `src/main/resources/application.yml`
- 各业务模块/组件可扩展自定义 `.yml`

---

## 2. 线程池参数配置

application.yml 示例：

```yaml
task:
executor:
corePoolSize: 5
maxPoolSize: 10
queueCapacity: 100
keepAliveSeconds: 60
threadNamePrefix: "task-executor-"
```

对应 application 层  
`TaskExecutorProperties.java`  
`TaskConfig.java`

---

## 3. WebClient 配置

```yaml
webclient:
connect-timeout: 10000
read-timeout: 10000
write-timeout: 10000
```

对应  
`WebClientConfig.java`  
支持通过 yml 修改超时时间等参数。

---

## 4. MyBatis-Plus 相关配置

数据库和代码生成相关配置在  
`infra/src/main/resources/mybatis-plus.yml`

```yaml
url: jdbc:mysql://localhost:3306/demo
username: root
password: root
output-path: ./src/generated
parent-package: com.example.project
mapper-path: ./src/generated/mapper
```

---

## 5. 多环境切换

- 建议采用 `application-{profile}.yml` 方案区分 dev、test、prod 配置。
- 启动时通过 `--spring.profiles.active=dev` 指定环境。

---

## 6. 扩展建议

- 配置类请放置在 application/config 或 application/properties 目录。
- 推荐使用 `@ConfigurationProperties` 自动注入配置参数。
- 所有通用配置（如日志、错误码）建议在 shared/ 维护并文档化。

---

## 7. 常见配置项汇总

- `server.port` - 服务端口
- `spring.datasource.*` - 数据源
- `logging.level.*` - 日志级别
- `task.executor.*` - 线程池参数
- `webclient.*` - WebClient 参数
- `mybatis-plus.*` - 持久化参数

---

## 8. 文档链接索引

- [架构设计说明](./architecture.md)
- [开发指南](./usage-guide.md)
- [对象分层说明](./object-layering.md)
- [测试指南](./test-guide.md)

---

如需添加新的配置项，请补充在本文件并在代码中做好注释。