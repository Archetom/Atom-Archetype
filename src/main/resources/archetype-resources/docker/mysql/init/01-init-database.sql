-- set client connection character set to utf8mb4
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- create database
CREATE DATABASE IF NOT EXISTS atom_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- use database
USE atom_db;

-- create user table
CREATE TABLE IF NOT EXISTS t_user
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ' user ID ',
    username VARCHAR(50) NOT NULL COMMENT ' username ',
    email VARCHAR(100) NOT NULL COMMENT ' email ',
    phone_number VARCHAR(20) NULL COMMENT ' phone number ',
    password VARCHAR(255) NOT NULL COMMENT 'password (encrypted)',
    real_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT ' real name ',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'status: ACTIVE-active, INACTIVE-inactive, LOCKED-locked, DELETED-deleted',
    external_id VARCHAR(100) NULL COMMENT ' external system ID ',
    is_external_user BOOLEAN NOT NULL DEFAULT FALSE COMMENT ' whether External User',
    is_admin BOOLEAN NOT NULL DEFAULT FALSE COMMENT ' whether administrator ',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ' tenant ID ',
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' created time ',
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' updated time ',
    deleted_time TIMESTAMP NULL COMMENT 'deleted time (logical delete)',

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
    COMMENT = ' user table ';

-- insert test data
INSERT INTO t_user (username, email, password, real_name, status, external_id, is_external_user, is_admin, tenant_id)
VALUES ('admin', 'admin@example.com', 'hashed_password_123', 'System Administrator', 'ACTIVE', NULL, FALSE, TRUE, 1),
       ('testuser', 'test@example.com', 'hashed_password_456', 'Test User', 'ACTIVE', NULL, FALSE, FALSE, 1),
       ('external_user', 'external@example.com', 'temp_password_789', 'External User', 'INACTIVE', 'EXT_001', TRUE, FALSE,
        1);
