# Atom Archetype

Atom 脚手架

![Requirement](https://img.shields.io/badge/JDK-17+-green.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.4-brightgreen.svg)
![atom version](https://img.shields.io/badge/Atom_Archetype-1.0.1-blue)

Atom 脚手架是一个基于 DDD (Domain-Driven Design) 设计理念构建的 Java 项目开发模板，支持快速生成高可维护性、高扩展性的工程结构。

## 快速开始
### 安装脚手架
使用 Maven 快速生成项目模板：

``` bash
mvn archetype:generate                  \
  -DarchetypeGroupId=io.github.archetom \
  -DarchetypeArtifactId=atom-archetype  \
  -DarchetypeVersion=1.0.1              \
  -DgroupId=com.foo.bar                 \
  -DartifactId=demo                     \
  -Dpackage=com.foo.bar                 \
  -Dversion=1.0.0-SNAPSHOT              \
  -B
```

生成成功后，项目结构如下：
```
demo
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

### 启动项目
``` bash
mvn clean install
cd start
mvn spring-boot:run
```

### 系统架构

```
                +----------------+
                |      api       |  <-- 系统对外暴露服务声明
                +----------------+
                        |
                        v
                +----------------+
                |  application   |  <-- 业务编排层
                +----------------+
                        |
                        v
                +----------------+
                |    domain      |  <-- 核心领域层
                +----------------+
                        |
       +----------------+----------------+
       |                                 |
+----------------+              +----------------------+
| infrastructure | <--------->  |      shared          |  <-- 公共工具和基础设施
+----------------+              +----------------------+
       |
+--------------+---------+---------+---------+---------+
| external     | messaging | persistence | rest    | rpc |
+--------------+---------+---------+---------+---------+
                        |
                        v
                +----------------+
                |     start      |  <-- 启动模块
                +----------------+
```