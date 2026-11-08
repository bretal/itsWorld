-- ========================================
-- 06 储值钱包域（账户 / 批次 / 充值 / 流水）
-- ========================================
-- 金额统一使用 BIGINT 整数分；资金流水追加式，禁止物理删除。
-- 本阶段不含钱包支付、退款、分摊、对账。
-- 本脚本只对空数据卷自动执行一次；已有数据卷需手动执行一次。
-- ========================================

USE its_world;

-- 充值方案
CREATE TABLE IF NOT EXISTS wallet_recharge_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '方案ID',
  plan_name VARCHAR(50) NOT NULL COMMENT '方案名称',
  principal_amount BIGINT NOT NULL COMMENT '本金金额(分)',
  bonus_amount BIGINT NOT NULL DEFAULT 0 COMMENT '赠送金额(分)',
  bonus_valid_days INT NOT NULL DEFAULT 0 COMMENT '赠送余额有效天数(0表示长期)',
  description VARCHAR(255) COMMENT '方案说明',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1启用 0停用)',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值方案表';

-- 钱包账户
CREATE TABLE IF NOT EXISTS wallet_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '钱包ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  principal_balance BIGINT NOT NULL DEFAULT 0 COMMENT '本金余额(分，含冻结)',
  bonus_balance BIGINT NOT NULL DEFAULT 0 COMMENT '赠送余额(分，含冻结)',
  principal_frozen BIGINT NOT NULL DEFAULT 0 COMMENT '本金冻结(分)',
  bonus_frozen BIGINT NOT NULL DEFAULT 0 COMMENT '赠送冻结(分)',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1正常 0冻结)',
  version INT NOT NULL DEFAULT 0 COMMENT '版本号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_user_id (user_id),
  CONSTRAINT fk_wallet_account_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包账户表';

-- 充值单
CREATE TABLE IF NOT EXISTS wallet_recharge_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '充值单ID',
  recharge_no VARCHAR(40) NOT NULL COMMENT '充值单号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  plan_id BIGINT COMMENT '充值方案ID',
  principal_amount BIGINT NOT NULL COMMENT '本金金额(分)',
  bonus_amount BIGINT NOT NULL DEFAULT 0 COMMENT '赠送金额(分)',
  channel VARCHAR(20) NOT NULL COMMENT '渠道(ONLINE/OFFLINE)',
  payment_method VARCHAR(20) COMMENT '支付方式(WECHAT/MOCK/OFFLINE)',
  store_name VARCHAR(100) COMMENT '线下门店名称',
  operator_id BIGINT COMMENT '创建人(线下充值)',
  approver_id BIGINT COMMENT '审批人(线下充值)',
  approved_at TIMESTAMP NULL COMMENT '审批时间',
  reject_reason VARCHAR(255) COMMENT '驳回原因',
  external_tx_no VARCHAR(100) COMMENT '外部交易号',
  payment_status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '支付状态(CREATED/PAID/CLOSED/EXCEPTION)',
  credit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '入账状态(PENDING/CREDITED)',
  plan_snapshot TEXT COMMENT '方案快照JSON',
  remark VARCHAR(255) COMMENT '备注',
  paid_at TIMESTAMP NULL COMMENT '支付时间',
  credited_at TIMESTAMP NULL COMMENT '入账时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_recharge_no (recharge_no),
  KEY idx_user_id (user_id),
  KEY idx_channel_status (channel, payment_status),
  KEY idx_created_at (created_at),
  CONSTRAINT fk_wallet_recharge_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_wallet_recharge_plan FOREIGN KEY (plan_id) REFERENCES wallet_recharge_plan(id) ON DELETE SET NULL,
  CONSTRAINT fk_wallet_recharge_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT fk_wallet_recharge_approver FOREIGN KEY (approver_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值单表';

-- 资金批次
CREATE TABLE IF NOT EXISTS wallet_fund_batch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '批次ID',
  wallet_id BIGINT NOT NULL COMMENT '钱包ID',
  recharge_order_id BIGINT COMMENT '来源充值单ID',
  fund_type VARCHAR(20) NOT NULL COMMENT '资金类型(PRINCIPAL/BONUS)',
  original_amount BIGINT NOT NULL COMMENT '原始金额(分)',
  remaining_amount BIGINT NOT NULL COMMENT '剩余金额(分，含冻结)',
  frozen_amount BIGINT NOT NULL DEFAULT 0 COMMENT '冻结金额(分)',
  expire_at TIMESTAMP NULL COMMENT '过期时间',
  scope VARCHAR(50) NOT NULL DEFAULT 'ALL' COMMENT '适用范围',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1有效 0失效)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_wallet_id (wallet_id),
  KEY idx_recharge_order_id (recharge_order_id),
  KEY idx_expire_at (expire_at),
  CONSTRAINT fk_wallet_batch_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id) ON DELETE CASCADE,
  CONSTRAINT fk_wallet_batch_order FOREIGN KEY (recharge_order_id) REFERENCES wallet_recharge_order(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包资金批次表';

-- 资金交易主流水（追加式）
CREATE TABLE IF NOT EXISTS wallet_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
  tx_no VARCHAR(40) NOT NULL COMMENT '流水号',
  wallet_id BIGINT NOT NULL COMMENT '钱包ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  tx_type VARCHAR(30) NOT NULL COMMENT '交易类型(RECHARGE/ADJUSTMENT/PAYMENT/REFUND)',
  business_type VARCHAR(30) COMMENT '业务类型',
  business_no VARCHAR(64) COMMENT '业务单号',
  idempotency_key VARCHAR(100) NOT NULL COMMENT '幂等键',
  principal_delta BIGINT NOT NULL DEFAULT 0 COMMENT '本金变动(分)',
  bonus_delta BIGINT NOT NULL DEFAULT 0 COMMENT '赠送变动(分)',
  principal_before BIGINT NOT NULL DEFAULT 0 COMMENT '变动前本金(分)',
  principal_after BIGINT NOT NULL DEFAULT 0 COMMENT '变动后本金(分)',
  bonus_before BIGINT NOT NULL DEFAULT 0 COMMENT '变动前赠送(分)',
  bonus_after BIGINT NOT NULL DEFAULT 0 COMMENT '变动后赠送(分)',
  remark VARCHAR(255) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_tx_no (tx_no),
  UNIQUE KEY uk_idempotency_key (idempotency_key),
  KEY idx_wallet_id (wallet_id),
  KEY idx_user_created (user_id, created_at),
  CONSTRAINT fk_wallet_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id) ON DELETE CASCADE,
  CONSTRAINT fk_wallet_transaction_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包资金交易主流水表';

-- 资金交易批次明细
CREATE TABLE IF NOT EXISTS wallet_transaction_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
  transaction_id BIGINT NOT NULL COMMENT '流水ID',
  batch_id BIGINT COMMENT '资金批次ID',
  fund_type VARCHAR(20) NOT NULL COMMENT '资金类型(PRINCIPAL/BONUS)',
  amount_delta BIGINT NOT NULL COMMENT '金额变动(分)',
  frozen_delta BIGINT NOT NULL DEFAULT 0 COMMENT '冻结变动(分)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_transaction_id (transaction_id),
  KEY idx_batch_id (batch_id),
  CONSTRAINT fk_wallet_detail_transaction FOREIGN KEY (transaction_id) REFERENCES wallet_transaction(id) ON DELETE CASCADE,
  CONSTRAINT fk_wallet_detail_batch FOREIGN KEY (batch_id) REFERENCES wallet_fund_batch(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包资金交易批次明细表';

-- ========================================
-- 种子数据
-- ========================================

INSERT IGNORE INTO wallet_recharge_plan
(id, plan_name, principal_amount, bonus_amount, bonus_valid_days, description, status, sort_order, created_at, updated_at)
VALUES
(1, '充100赠10', 10000, 1000, 30, '实充 100 元，赠送 10 元', 1, 1, NOW(), NOW()),
(2, '充500赠80', 50000, 8000, 60, '实充 500 元，赠送 80 元', 1, 2, NOW(), NOW()),
(3, '充1000赠200', 100000, 20000, 90, '实充 1000 元，赠送 200 元', 1, 3, NOW(), NOW());
