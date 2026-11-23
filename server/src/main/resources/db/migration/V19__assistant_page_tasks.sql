CREATE TABLE assistant_page_task (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_page_task_request (user_id, request_id),
    KEY idx_page_task_user_time (user_id, created_at)
);
