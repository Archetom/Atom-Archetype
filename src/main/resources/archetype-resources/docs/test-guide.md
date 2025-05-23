# 测试指南

本指南描述如何在本脚手架下进行单元测试和集成测试，推荐规范和实践建议。

---

## 1. 测试分层结构

- `/test/java/` 目录下分别与 main/java 下各模块结构对应。
- 推荐每个 service/assembler/domain/entity/infra/repository 等都配套测试类。

---

## 2. 单元测试规范

- 业务核心逻辑应覆盖单元测试，确保各方法边界条件和主流程可测。
- 推荐使用 JUnit 5。
- 可用 Mockito/MockMvc 等工具模拟依赖。

```java
@Test
void should_return_true_when_user_valid() {
// arrange
// act
// assert
}
```

---

## 3. 集成测试建议

- 建议 application/infra 层关键链路配集成测试，包含数据库或外部服务集成。
- 可用 SpringBootTest 注解，结合测试容器如 Testcontainers 或 H2。

```java
@SpringBootTest
class UserServiceIntegrationTest {
// ...
}
```

---

## 4. Mock 与数据准备

- 尽量 Mock 掉外部依赖（如 MQ、第三方接口）。
- 测试数据可放置在 `src/test/resources/` 下，统一管理。

---

## 5. 代码覆盖率与持续集成

- 建议引入 Jacoco 或 Coverage 插件统计覆盖率，主业务代码建议 80%+。
- 可配置到 CI（如 Github Actions、Jenkins）自动执行测试。

---

## 6. 常见问题

- 测试执行失败，优先排查依赖是否 mock、数据是否初始化。
- 并发/异步相关测试建议加超时控制，防止死锁或假死。

---

## 7. 参考示例

- `application/src/test/java/application/service/UserServiceTest.java`
- `domain/src/test/java/domain/entity/UserTest.java`

---

## 8. 文档链接索引

- [架构设计说明](./architecture.md)
- [开发指南](./usage-guide.md)
- [配置说明](./configuration.md)
- [对象分层说明](./object-layering.md)

---

如遇特殊业务场景，请补充用例并及时完善本指南。