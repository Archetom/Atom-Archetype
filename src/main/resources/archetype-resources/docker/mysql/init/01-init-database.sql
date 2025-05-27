-- 设置客户端连接字符集为 utf8mb4
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS atom_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE atom_db;

-- 创建用户表
CREATE TABLE IF NOT EXISTS t_user
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username         VARCHAR(50)  NOT NULL COMMENT '用户名',
    email            VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone_number     VARCHAR(20)  NULL COMMENT '手机号',
    password         VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    real_name        VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '真实姓名',
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-激活，INACTIVE-未激活，LOCKED-锁定，DELETED-已删除',
    external_id      VARCHAR(100) NULL COMMENT '外部系统ID',
    is_external_user BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否外部用户',
    is_admin         BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否管理员',
    tenant_id        BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    created_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_time     TIMESTAMP    NULL COMMENT '删除时间（逻辑删除）',

    UNIQUE KEY uk_username_tenant (username, tenant_id),
    UNIQUE KEY uk_email_tenant (email, tenant_id),
    INDEX idx_phone_number (phone_number),
    INDEX idx_status (status),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_created_time (created_time),
    INDEX idx_external_id (external_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '用户表';

-- 插入测试数据
INSERT INTO t_user (username, email, password, real_name, status, external_id, is_external_user, is_admin, tenant_id)
VALUES ('admin', 'admin@example.com', 'hashed_password_123', '系统管理员', 'ACTIVE', NULL, FALSE, TRUE, 1),
       ('testuser', 'test@example.com', 'hashed_password_456', '测试用户', 'ACTIVE', NULL, FALSE, FALSE, 1),
       ('external_user', 'external@example.com', 'temp_password_789', '外部用户', 'INACTIVE', 'EXT_001', TRUE, FALSE,
        1);
