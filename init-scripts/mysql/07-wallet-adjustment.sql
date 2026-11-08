-- ========================================
-- 07 钱包余额调账审计
-- ========================================
-- 管理员调增/调减用户本金或赠送余额；必须填写原因。
-- 本脚本只对空数据卷自动执行一次；已有数据卷需手动执行一次。
-- ========================================

USE its_world;

CREATE TABLE IF NOT EXISTS wallet_adjustment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '调账ID',
  adjustment_no VARCHAR(40) NOT NULL COMMENT '调账单号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  principal_delta BIGINT NOT NULL DEFAULT 0 COMMENT '本金变动(分，可为负)',
  bonus_delta BIGINT NOT NULL DEFAULT 0 COMMENT '赠送变动(分，可为负)',
  principal_before BIGINT NOT NULL DEFAULT 0 COMMENT '调账前本金(分)',
  principal_after BIGINT NOT NULL DEFAULT 0 COMMENT '调账后本金(分)',
  bonus_before BIGINT NOT NULL DEFAULT 0 COMMENT '调账前赠送(分)',
  bonus_after BIGINT NOT NULL DEFAULT 0 COMMENT '调账后赠送(分)',
  reason VARCHAR(255) NOT NULL COMMENT '调账原因',
  operator_id BIGINT NOT NULL COMMENT '操作人ID',
  status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态(SUCCESS/FAILED)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_adjustment_no (adjustment_no),
  KEY idx_user_created (user_id, created_at),
  KEY idx_operator_id (operator_id),
  CONSTRAINT fk_wallet_adjustment_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_wallet_adjustment_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包余额调账审计表';
