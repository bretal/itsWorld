-- 管理操作留痕与举报工单；可重复执行，仅新增表。
CREATE TABLE IF NOT EXISTS admin_moderation_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 target_type VARCHAR(32) NOT NULL,
 target_id BIGINT NOT NULL,
 action VARCHAR(32) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 admin_id BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_target (target_type,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS content_report (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 target_type VARCHAR(32) NOT NULL,
 target_id BIGINT NOT NULL,
 reporter_id BIGINT NULL,
 reason VARCHAR(500) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 resolution VARCHAR(500) NULL,
 handled_by BIGINT NULL,
 handled_at TIMESTAMP NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_status_created (status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
