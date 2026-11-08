-- ========================================
-- 10 钱包业务支付/退款扩展（复用现有 wallet_account / fund_batch / transaction）
-- ========================================
-- 不新建钱包账户/批次/流水表；新增支付分摊与退款追溯表。
-- 依赖 09-commerce-order.sql 的 commerce_payment_link。
-- ALTER user_coupon 非幂等，同一库只执行一次。
-- ========================================

USE its_world;

-- 钱包支付批次分摊
CREATE TABLE IF NOT EXISTS wallet_payment_allocation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分摊ID',
  pay_order_id BIGINT NOT NULL COMMENT '商业支付记录ID(commerce_payment_link.id)',
  wallet_tx_id BIGINT NOT NULL COMMENT '钱包流水ID',
  batch_id BIGINT NULL COMMENT '钱包资金批次ID',
  fund_type VARCHAR(20) NOT NULL COMMENT '资金类型(PRINCIPAL/BONUS)',
  paid_amount BIGINT NOT NULL COMMENT '支付金额(分)',
  refunded_amount BIGINT NOT NULL DEFAULT 0 COMMENT '已退金额(分)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_pay_order (pay_order_id),
  KEY idx_wallet_tx (wallet_tx_id),
  KEY idx_batch (batch_id),
  CONSTRAINT fk_wallet_alloc_pay FOREIGN KEY (pay_order_id) REFERENCES commerce_payment_link(id) ON DELETE CASCADE,
  CONSTRAINT fk_wallet_alloc_tx FOREIGN KEY (wallet_tx_id) REFERENCES wallet_transaction(id),
  CONSTRAINT fk_wallet_alloc_batch FOREIGN KEY (batch_id) REFERENCES wallet_fund_batch(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包支付批次分摊表';

-- 钱包退款单
CREATE TABLE IF NOT EXISTS wallet_refund (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '退款ID',
  refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
  pay_order_id BIGINT NOT NULL COMMENT '商业支付记录ID',
  business_order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
  amount BIGINT NOT NULL COMMENT '退款金额(分)',
  status VARCHAR(24) NOT NULL DEFAULT 'REQUESTED' COMMENT '状态(REQUESTED/SUCCESS/FAILED)',
  reason VARCHAR(255) COMMENT '退款原因',
  external_refund_no VARCHAR(100) COMMENT '外部退款号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_pay_order (pay_order_id),
  KEY idx_business_order (business_order_no),
  CONSTRAINT fk_wallet_refund_pay FOREIGN KEY (pay_order_id) REFERENCES commerce_payment_link(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包退款单表';

-- 钱包退款原分摊追溯
CREATE TABLE IF NOT EXISTS wallet_refund_allocation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '退款分摊ID',
  refund_id BIGINT NOT NULL COMMENT '退款ID',
  original_payment_allocation_id BIGINT NOT NULL COMMENT '原支付分摊ID',
  amount BIGINT NOT NULL COMMENT '退款金额(分)',
  restored_batch_id BIGINT NULL COMMENT '恢复后的资金批次ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_refund (refund_id),
  KEY idx_original_alloc (original_payment_allocation_id),
  KEY idx_restored_batch (restored_batch_id),
  CONSTRAINT fk_refund_alloc_refund FOREIGN KEY (refund_id) REFERENCES wallet_refund(id) ON DELETE CASCADE,
  CONSTRAINT fk_refund_alloc_original FOREIGN KEY (original_payment_allocation_id) REFERENCES wallet_payment_allocation(id),
  CONSTRAINT fk_refund_alloc_batch FOREIGN KEY (restored_batch_id) REFERENCES wallet_fund_batch(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包退款原分摊追溯表';

-- 优惠券锁定/核销字段（V2 需要，现有表缺）
ALTER TABLE user_coupon
  ADD COLUMN lock_order_no VARCHAR(64) NULL COMMENT '锁定订单号' AFTER used_at,
  ADD COLUMN used_order_no VARCHAR(64) NULL COMMENT '核销订单号' AFTER lock_order_no,
  ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '版本号' AFTER used_order_no;
