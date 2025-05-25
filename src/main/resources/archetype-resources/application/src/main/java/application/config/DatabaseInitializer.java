package ${package}.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initDatabase() {
        try {
            // 检查表是否存在
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user WHERE 1=0", Integer.class);
            System.out.println("表 t_user 已存在");
        } catch (Exception e) {
            // 表不存在，创建表
            System.out.println("创建表 t_user...");

            String createTableSql = """
                CREATE TABLE t_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    real_name VARCHAR(100),
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    tenant_id BIGINT,
                    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted_time TIMESTAMP NULL
                )
                """;

            jdbcTemplate.execute(createTableSql);

            // 创建索引
            try {
                jdbcTemplate.execute("CREATE UNIQUE INDEX uk_username ON t_user(username)");
                jdbcTemplate.execute("CREATE UNIQUE INDEX uk_email ON t_user(email)");
                jdbcTemplate.execute("CREATE INDEX idx_status ON t_user(status)");
                jdbcTemplate.execute("CREATE INDEX idx_created_time ON t_user(created_time)");
            } catch (Exception ex) {
                // 忽略索引创建错误
            }

            System.out.println("表 t_user 创建完成！");
        }
    }
}
