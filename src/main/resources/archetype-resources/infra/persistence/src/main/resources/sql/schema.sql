-- user table
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ' user ID ',
    username VARCHAR(50) NOT NULL COMMENT ' username ',
    email VARCHAR(100) NOT NULL COMMENT ' email ',
    password VARCHAR(255) NOT NULL COMMENT 'password (encrypted)',
    real_name VARCHAR(100) COMMENT ' real name ',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'status: ACTIVE-active, INACTIVE-inactive, LOCKED-locked, DELETED-deleted',
    tenant_id BIGINT COMMENT ' tenant ID ',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' created time ',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' updated time ',
    deleted_time DATETIME NULL COMMENT 'deleted time (logical delete)',
    
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' user table ';
