# Atom Archetype

![Requirement](https://img.shields.io/badge/JDK-21+-green.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.0-brightgreen.svg)
![atom version](https://img.shields.io/badge/Atom_Archetype-1.0.1-blue)

> 基于 DDD 设计理念的 Java 项目脚手架，专为构建高可维护性、高扩展性的现代化应用而生。

## ✨ 特性

- 🏗️ **DDD 分层架构** - 清晰的领域驱动设计分层，职责明确
- 🚀 **开箱即用** - 集成 Spring Boot 3.5 + JDK 21，现代化技术栈
- 🔧 **服务模板** - 内置责任链模式，统一业务处理流程
- 📦 **多模块设计** - api/application/domain/infra/shared 清晰分离
- 🧪 **测试友好** - 集成 Testcontainers，支持容器化测试
- ⚡  **性能优化** - 内置缓存、分布式锁、线程池等基础设施

## 🚀 快速开始

### 生成项目

```bash
mvn archetype:generate \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype \
  -DarchetypeVersion=1.0.1 \
  -DgroupId=com.foo.bar \
  -DartifactId=demo \
  -Dpackage=com.foo.bar \
  -Dversion=1.0.0-SNAPSHOT \
  -B
```

### 启动项目
#### 1. 启动 MySQL 和 Redis
```bash
docker-compose up -d
```

#### 2. 安装并启动项目
```bash
mvn clean install
cd start
mvn spring-boot:run
```

## 📁 项目结构

```
demo/
├── api/                  # 对外服务声明层
├── application/          # 应用层，业务编排
├── domain/               # 领域层，核心业务
├── infra/                # 基础设施层
│   ├── external/         # 外部服务集成
│   ├── messaging/        # 消息队列
│   ├── persistence/      # 数据持久化
│   ├── rest/             # REST 接口
│   └── rpc/              # RPC 接口
├── shared/               # 共享组件
└── start/                # 启动模块
```

## 🛠️ 技术栈

- **框架**: Spring Boot 3.5.0
- **数据库**: MyBatis-Plus + MySQL
- **缓存**: Redis
- **测试**: JUnit 5 + Testcontainers
- **构建**: Maven

## API 示例

### 创建用户
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user007",
    "email": "user007@example.com",
    "password": "password123",
    "realName": "User007"
  }'
```

## 📖 文档

- 🏗️ [架构设计](docs/architecture.md) - 整体架构和设计理念
- 📋 [开发指南](docs/usage-guide.md) - 日常开发流程和示例
- ⚙️ [配置说明](docs/configuration.md) - 各模块配置参数
- 📊 [对象分层](docs/object-layering.md) - DTO/VO/PO 使用规范
- 🧪 [测试指南](docs/test-guide.md) - 单元测试和集成测试

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

[MIT License](LICENSE)

