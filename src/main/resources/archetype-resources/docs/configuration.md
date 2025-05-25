# 配置说明

## 核心配置

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
```

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### 线程池配置

```yaml
task:
  executor:
    core-pool-size: 5          # 核心线程数
    max-pool-size: 10          # 最大线程数
    queue-capacity: 100        # 队列容量
    keep-alive-seconds: 60     # 线程存活时间
    thread-name-prefix: "task-executor-"
```

### HTTP 客户端配置

```yaml
webclient:
  connect-timeout: 10000      # 连接超时（毫秒）
  read-timeout: 10000         # 读取超时（毫秒）
  write-timeout: 10000        # 写入超时（毫秒）
```

## 环境配置

### 开发环境 (application-dev.yml)

```yaml
spring:
  profiles:
    active: dev
    
logging:
  level:
    com.example: DEBUG
    org.springframework.web: DEBUG
    
server:
  port: 8080
```

### 测试环境 (application-test.yml)

```yaml
spring:
  profiles:
    active: test
    
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    
logging:
  level:
    root: WARN
    com.example: INFO
```

### 生产环境 (application-prod.yml)

```yaml
spring:
  profiles:
    active: prod
    
server:
  port: 8080
  
logging:
  level:
    root: INFO
  file:
    name: /var/log/app/application.log
```

## MyBatis-Plus 配置

### 代码生成配置

```yaml
# infra/persistence/src/main/resources/mybatis-plus.yml
url: jdbc:mysql://localhost:3306/demo
username: root
password: root
output-path: ./infra/persistence/src/main/java
parent-package: com.example.infra.persistence.mysql
mapper-path: ./infra/persistence/src/main/resources/mapper
```

### 分页和审计配置

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
  global-config:
    db-config:
      logic-delete-field: deletedTime
      logic-delete-value: now()
      logic-not-delete-value: 'null'
```

## 监控配置

### Actuator 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

### 日志配置

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 100MB
    max-history: 30
```

## 自定义配置

### 业务配置示例

```java
@Data
@ConfigurationProperties(prefix = "app.business")
public class BusinessProperties {
    private String apiBaseUrl = "https://api.example.com";
    private int maxRetryTimes = 3;
    private Duration timeout = Duration.ofSeconds(30);
    private boolean enableCache = true;
}
```

```yaml
app:
  business:
    api-base-url: https://api.example.com
    max-retry-times: 3
    timeout: 30s
    enable-cache: true
```

## 配置最佳实践

### 敏感信息处理

```yaml
# 使用环境变量
spring:
  datasource:
    password: ${DB_PASSWORD:default_password}
    
# 使用配置文件加密
jasypt:
  encryptor:
    password: ${JASYPT_PASSWORD}
```

### 配置验证

```java
@ConfigurationProperties(prefix = "app.feature")
@Validated
public class FeatureProperties {
    @NotBlank
    private String apiUrl;
    
    @Min(1)
    @Max(100)
    private int maxConnections = 10;
}
```

### 条件配置

```java
@Configuration
@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")
public class FeatureConfig {
    // 配置内容
}
```

## 常见配置问题

### Q: 如何在不同环境使用不同配置？

A: 使用 Spring Profile：

```bash
# 启动时指定环境
java -jar app.jar --spring.profiles.active=prod

# 或设置环境变量
export SPRING_PROFILES_ACTIVE=prod
```

### Q: 如何动态刷新配置？

A: 使用 `@RefreshScope` 注解：

```java
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.dynamic")
public class DynamicConfig {
    private String value;
}
```

### Q: 如何验证配置是否正确？

A: 添加配置验证：

```java
@PostConstruct
public void validateConfig() {
    if (StringUtils.isBlank(apiUrl)) {
        throw new IllegalStateException("API URL must be configured");
    }
}
```
