-- 设置客户端连接字符集为 utf8mb4
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS atom_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE atom_db;

-- 创建用户表
CREATE TABLE IF NOT EXISTS t_user
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL,
    email        VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20)  NULL,
    password     VARCHAR(255) NOT NULL,
    real_name    VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    tenant_id    BIGINT       NOT NULL DEFAULT 1,
    created_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_time TIMESTAMP    NULL,

    UNIQUE KEY uk_username_tenant (username, tenant_id),
    UNIQUE KEY uk_email_tenant (email, tenant_id),
    INDEX idx_phone_number (phone_number),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_created_time (created_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 插入测试数据
INSERT INTO t_user (username, email, password, real_name, status, tenant_id)
VALUES ('admin', 'admin@example.com', 'hashed_password_123', '系统管理员', 'ACTIVE', 1);
