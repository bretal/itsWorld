-- 社区帖子点赞表（增量迁移）
-- 已有数据卷不会重新执行 01-init.sql，需要手动执行本脚本。

CREATE TABLE IF NOT EXISTS community_like (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
  post_id BIGINT NOT NULL COMMENT '帖子ID',
  user_id BIGINT NOT NULL COMMENT '点赞用户ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_post_user (post_id, user_id),
  KEY idx_user_id (user_id),
  FOREIGN KEY (post_id) REFERENCES community_post(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子点赞表';
