-- Additive migration; existing commerce data is preserved.
CREATE TABLE IF NOT EXISTS assistant_session (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 title VARCHAR(60) NOT NULL DEFAULT '新对话',
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_assistant_session_user (user_id, updated_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS assistant_turn (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 session_id BIGINT NOT NULL,
 request_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
 content_hash CHAR(64) NOT NULL,
 question TEXT NOT NULL,
 answer TEXT NULL,
 products_json JSON NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 attempt_count INT NOT NULL DEFAULT 1,
 lease_token VARCHAR(64) NULL,
 lease_until DATETIME NULL,
 model VARCHAR(64) NULL,
 prompt_tokens INT NULL,
 completion_tokens INT NULL,
 error_code VARCHAR(40) NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_assistant_request (user_id, request_id),
 KEY idx_assistant_turn_session (session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS assistant_user_quota (
 user_id BIGINT NOT NULL PRIMARY KEY,
 hour_start DATETIME NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 active_request_id VARCHAR(64) NULL,
 lease_token VARCHAR(64) NULL,
 lease_until DATETIME NULL,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
