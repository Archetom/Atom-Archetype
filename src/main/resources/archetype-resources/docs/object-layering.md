# 分层对象规范（DTO/VO/PO/Request/Response 使用说明）

## 一、概念区分

- **DTO（Data Transfer Object）数据传输对象**
    - 用于服务、模块、微服务之间的数据传递。
    - 不包含业务逻辑，专注数据结构。

- **VO（View Object / Value Object）视图对象 / 值对象**
    - 面向前端或其他接口展示，用于数据返回和渲染。
    - 仅包含页面/接口需要的字段。

- **PO（Persistent Object）持久化对象**
    - 映射数据库表结构，仅做数据持久化，不能直接用于接口。

- **Request / Response（请求/响应对象）**
    - rest 层专用对象，和 DTO/VO 一一对应。
    - **Request** 专用于接收前端/外部请求参数（输入），可理解为“输入 DTO”。
    - **Response** 专用于对外返回结果（输出），可理解为“输出 VO”。
    - 命名规范：如 `UserRequest`、`UserResponse`。
    - 通常每个接口的 Request/Response 成对出现，便于接口的参数与返回分离，代码更清晰。
    - 通常位于 `api/dto/request/`、`api/dto/response/`、`infra/rest/request/`、`infra/rest/response/` 等目录。

---

## 二、典型流转方向
```text
Controller/Facade
↓
Request（输入）
↓
DTO
↓
Service/Application
↓
Domain/PO（持久层）
↓
Repository/DAO
↑
VO / Response（输出）
↑
Controller/Facade
```
- rest 层只用 **Request/Response** 命名。
- DTO/VO 更常用于 application/domain 层及跨服务场景。

---

## 三、默认目录

- `api/dto/request/`  — 接口请求对象（Request）
- `api/dto/response/` — 接口返回对象（Response）
- `application/vo/`   — 业务返回对象（VO）
- `infra/persistence/mysql/po/` — 持久化对象（PO）

---

## 四、使用规范

1. **Request/Response 对象与 DTO/VO 区分使用，不混用**
2. **同一接口建议 Request/Response 成对出现**
3. **Request/Response 仅用于 rest/controller 层，不直接穿透到 domain/infra 层**
4. **PO 只做数据库操作，不暴露到前端**

---

## 五、团队协作建议

- 统一对象命名规范，Request/Response 明确表示接口输入输出。
- 代码评审时检查是否使用了正确的对象类型。
- Controller 层只接收 Request/返回 Response，禁止直接暴露 PO/DO。

---

## 六、示例结构
```java
// 请求参数对象
public class UserRequest {
    private String username;
    private String password;
    // ...
}
```

```java
// 持久化对象
public class UserPO {
    private Long id;
    private String username;
    // ...
}
```

```java
// 返回视图对象
public class UserResponse {
    private Long userId;
    private String displayName;
    // ...
}
```

---

## 七、常见误用举例

- ❌ Controller 方法直接接收/返回 PO/DO。
- ❌ 一个对象在 Request、Response、VO、DTO 间混用。
- ❌ 业务逻辑代码直接操作 Request/Response。

---

## 八、总结

- **Request/Response 专属 rest 层，确保接口解耦**
- **DTO/VO 适用于 service/application/domain 层**
- **PO/DO 专注持久化，绝不直接暴露**

---

如有更多对象分层、包结构或开发规范建议，请补充至本文档并通知团队。