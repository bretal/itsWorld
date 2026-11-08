-- ========================================
-- 12 商业服务物流、售后与评价
-- ========================================
-- 物流：发货单、发货明细。
-- 售后：售后单、售后明细、退款记录、状态日志。
-- 评价：订单评价与审核回复。
-- 已有数据卷需手动执行一次；新数据卷按 01→12 自动执行。
-- ========================================

USE its_world;

-- 发货单
CREATE TABLE IF NOT EXISTS commerce_shipment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '发货单ID',
  shipment_no VARCHAR(64) NOT NULL COMMENT '发货单号',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  delivery_company VARCHAR(64) NOT NULL COMMENT '物流公司',
  tracking_no VARCHAR(64) NOT NULL COMMENT '物流单号',
  status VARCHAR(24) NOT NULL DEFAULT 'SHIPPED' COMMENT '状态 SHIPPED/DELIVERED',
  shipped_time TIMESTAMP NULL COMMENT '发货时间',
  received_time TIMESTAMP NULL COMMENT '确认收货时间',
  remark VARCHAR(255) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_shipment_no (shipment_no),
  KEY idx_order (order_id),
  KEY idx_status_time (status, shipped_time),
  CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业发货单表';

-- 发货明细
CREATE TABLE IF NOT EXISTS commerce_shipment_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '发货明细ID',
  shipment_id BIGINT NOT NULL COMMENT '发货单ID',
  order_item_id BIGINT NOT NULL COMMENT '订单项ID',
  sku_id BIGINT NULL COMMENT 'SKU ID',
  qty INT NOT NULL DEFAULT 0 COMMENT '发货数量',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_shipment_item (shipment_id, order_item_id),
  KEY idx_order_item (order_item_id),
  CONSTRAINT fk_shipment_item_shipment FOREIGN KEY (shipment_id) REFERENCES commerce_shipment(id) ON DELETE CASCADE,
  CONSTRAINT fk_shipment_item_order_item FOREIGN KEY (order_item_id) REFERENCES commerce_order_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_shipment_item_sku FOREIGN KEY (sku_id) REFERENCES shop_sku(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业发货明细表';

-- 售后单
CREATE TABLE IF NOT EXISTS commerce_after_sale (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '售后单ID',
  after_sale_no VARCHAR(64) NOT NULL COMMENT '售后单号',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  type VARCHAR(24) NOT NULL COMMENT '类型 REFUND_ONLY/RETURN_REFUND',
  reason VARCHAR(255) NOT NULL COMMENT '申请原因',
  description TEXT COMMENT '问题描述',
  evidence_urls JSON NULL COMMENT '凭证图片',
  amount BIGINT NOT NULL COMMENT '申请退款金额(分)',
  status VARCHAR(24) NOT NULL DEFAULT 'APPLIED' COMMENT '状态 APPLIED/REVIEWING/APPROVED/REJECTED/REFUNDING/REFUNDED/REFUND_FAILED',
  review_remark VARCHAR(255) COMMENT '审核备注',
  reviewed_by BIGINT NULL COMMENT '审核人ID',
  reviewed_at TIMESTAMP NULL COMMENT '审核时间',
  refunded_at TIMESTAMP NULL COMMENT '退款完成时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_after_sale_no (after_sale_no),
  KEY idx_user_status (user_id, status),
  KEY idx_order (order_id),
  CONSTRAINT fk_after_sale_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_after_sale_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_after_sale_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业售后单表';

-- 售后明细
CREATE TABLE IF NOT EXISTS commerce_after_sale_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '售后明细ID',
  after_sale_id BIGINT NOT NULL COMMENT '售后单ID',
  order_item_id BIGINT NOT NULL COMMENT '订单项ID',
  sku_id BIGINT NULL COMMENT 'SKU ID',
  qty INT NOT NULL COMMENT '申请数量',
  amount BIGINT NOT NULL COMMENT '申请金额(分)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_after_sale_item (after_sale_id, order_item_id),
  KEY idx_order_item (order_item_id),
  CONSTRAINT fk_after_sale_item_after_sale FOREIGN KEY (after_sale_id) REFERENCES commerce_after_sale(id) ON DELETE CASCADE,
  CONSTRAINT fk_after_sale_item_order_item FOREIGN KEY (order_item_id) REFERENCES commerce_order_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_after_sale_item_sku FOREIGN KEY (sku_id) REFERENCES shop_sku(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业售后明细表';

-- 退款记录
CREATE TABLE IF NOT EXISTS commerce_refund_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '退款记录ID',
  refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
  after_sale_id BIGINT NOT NULL COMMENT '售后单ID',
  payment_link_id BIGINT NOT NULL COMMENT '原支付记录ID',
  channel VARCHAR(24) NOT NULL COMMENT '退款渠道 WALLET/MOCK_WECHAT/WECHAT',
  amount BIGINT NOT NULL COMMENT '退款金额(分)',
  status VARCHAR(24) NOT NULL DEFAULT 'CREATED' COMMENT '状态 CREATED/REFUNDED/FAILED',
  external_refund_no VARCHAR(128) COMMENT '外部退款单号',
  fail_reason VARCHAR(255) COMMENT '失败原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_after_sale (after_sale_id),
  KEY idx_payment_link (payment_link_id),
  CONSTRAINT fk_refund_link_after_sale FOREIGN KEY (after_sale_id) REFERENCES commerce_after_sale(id) ON DELETE CASCADE,
  CONSTRAINT fk_refund_link_payment FOREIGN KEY (payment_link_id) REFERENCES commerce_payment_link(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业退款记录表';

-- 售后状态日志
CREATE TABLE IF NOT EXISTS commerce_after_sale_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  after_sale_id BIGINT NOT NULL COMMENT '售后单ID',
  from_status VARCHAR(24) COMMENT '原状态',
  to_status VARCHAR(24) NOT NULL COMMENT '新状态',
  action VARCHAR(32) NOT NULL COMMENT '动作',
  operator_type VARCHAR(16) NOT NULL COMMENT '操作方 USER/ADMIN/SYSTEM',
  operator_id BIGINT NULL COMMENT '操作人ID',
  remark VARCHAR(255) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_after_sale_time (after_sale_id, created_at),
  CONSTRAINT fk_after_sale_log_after_sale FOREIGN KEY (after_sale_id) REFERENCES commerce_after_sale(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业售后状态日志表';

-- 订单评价
CREATE TABLE IF NOT EXISTS commerce_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  rating TINYINT NOT NULL DEFAULT 5 COMMENT '评分1-5',
  content VARCHAR(1000) COMMENT '评价内容',
  images JSON NULL COMMENT '评价图片',
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/APPROVED/HIDDEN',
  reply VARCHAR(500) COMMENT '商家回复',
  replied_at TIMESTAMP NULL COMMENT '回复时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_order (order_id),
  KEY idx_status_time (status, created_at),
  KEY idx_user (user_id),
  CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业订单评价表';
