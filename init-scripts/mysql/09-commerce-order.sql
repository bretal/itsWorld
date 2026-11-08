-- ========================================
-- 09 商业服务交易域（地址 / 购物车 / 订单 / 支付关联 / 库存预占）
-- ========================================
-- Phase 2：商品订单（PRODUCT）主链路；美容/套餐订单后续复用。
-- 金额 BIGINT 整数分；订单历史/支付记录不物理删除。
-- 已有数据卷需手动执行一次；新数据卷按 01→09 自动执行。
-- ========================================

USE its_world;

-- 收货地址
CREATE TABLE IF NOT EXISTS user_shipping_address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  recipient_name VARCHAR(64) NOT NULL COMMENT '收货人',
  mobile VARCHAR(32) NOT NULL COMMENT '手机号',
  province VARCHAR(64) COMMENT '省',
  city VARCHAR(64) COMMENT '市',
  district VARCHAR(64) COMMENT '区',
  address_detail VARCHAR(255) NOT NULL COMMENT '详细地址',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认(1是 0否)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_user (user_id),
  CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址表';

-- 购物车
CREATE TABLE IF NOT EXISTS shop_cart_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '购物车项ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  sku_id BIGINT NOT NULL COMMENT 'SKU ID',
  quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
  selected TINYINT NOT NULL DEFAULT 1 COMMENT '是否勾选(1是 0否)',
  invalid_reason VARCHAR(128) COMMENT '失效原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_user_sku (user_id, sku_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_sku FOREIGN KEY (sku_id) REFERENCES shop_sku(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- 统一业务订单
CREATE TABLE IF NOT EXISTS commerce_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
  order_no VARCHAR(64) NOT NULL COMMENT '订单号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  order_type VARCHAR(24) NOT NULL COMMENT '订单类型(PRODUCT/GROOMING/PACKAGE)',
  order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
  payment_status VARCHAR(24) NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态(UNPAID/PAID/PARTIAL_REFUND/REFUNDED)',
  fulfillment_status VARCHAR(24) NOT NULL DEFAULT 'PENDING' COMMENT '履约状态(PENDING/SHIPPED/RECEIVED)',
  original_amount BIGINT NOT NULL DEFAULT 0 COMMENT '原始金额(分)',
  discount_amount BIGINT NOT NULL DEFAULT 0 COMMENT '优惠金额(分)',
  payable_amount BIGINT NOT NULL DEFAULT 0 COMMENT '应付金额(分)',
  paid_amount BIGINT NOT NULL DEFAULT 0 COMMENT '实付金额(分)',
  refunded_amount BIGINT NOT NULL DEFAULT 0 COMMENT '已退金额(分)',
  coupon_id BIGINT NULL COMMENT '用户优惠券ID',
  address_snapshot JSON NULL COMMENT '收货地址快照',
  store_snapshot JSON NULL COMMENT '门店快照',
  expire_time TIMESTAMP NULL COMMENT '支付超时时间',
  paid_time TIMESTAMP NULL COMMENT '支付时间',
  close_time TIMESTAMP NULL COMMENT '关闭时间',
  cancel_reason VARCHAR(255) COMMENT '取消/关闭原因',
  remark VARCHAR(255) COMMENT '备注',
  idempotency_key VARCHAR(128) NOT NULL COMMENT '提交幂等键',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_user_idempotency (user_id, idempotency_key),
  KEY idx_user_status (user_id, order_status, created_at),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_coupon FOREIGN KEY (coupon_id) REFERENCES user_coupon(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业业务订单表';

-- 订单项（价格/规格快照）
CREATE TABLE IF NOT EXISTS commerce_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单项ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  item_type VARCHAR(24) NOT NULL COMMENT '订单项类型(PRODUCT/GROOMING/PACKAGE)',
  target_id BIGINT COMMENT '业务对象ID(商品/服务/套餐)',
  sku_id BIGINT NULL COMMENT 'SKU ID',
  pet_id BIGINT NULL COMMENT '宠物ID',
  quantity INT NOT NULL COMMENT '数量',
  title_snapshot VARCHAR(255) NOT NULL COMMENT '标题快照',
  spec_snapshot JSON NULL COMMENT '规格快照',
  unit_price BIGINT NOT NULL COMMENT '单价(分)',
  original_total BIGINT NOT NULL COMMENT '原始小计(分)',
  discount_allocated BIGINT NOT NULL DEFAULT 0 COMMENT '分摊优惠(分)',
  payable_total BIGINT NOT NULL COMMENT '应付小计(分)',
  fulfilled_qty INT NOT NULL DEFAULT 0 COMMENT '已履约数量',
  refunded_amount BIGINT NOT NULL DEFAULT 0 COMMENT '已退金额(分)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_order (order_id),
  KEY idx_target (item_type, target_id),
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_item_sku FOREIGN KEY (sku_id) REFERENCES shop_sku(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业订单项表';

-- 订单优惠明细
CREATE TABLE IF NOT EXISTS commerce_order_discount (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '优惠ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  order_item_id BIGINT NULL COMMENT '订单项ID',
  discount_type VARCHAR(32) NOT NULL COMMENT '优惠类型(MEMBER/COUPON/OTHER)',
  source_id BIGINT NULL COMMENT '来源ID',
  discount_amount BIGINT NOT NULL COMMENT '优惠金额(分)',
  rule_snapshot JSON NULL COMMENT '规则快照',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_order (order_id),
  CONSTRAINT fk_order_discount_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_discount_item FOREIGN KEY (order_item_id) REFERENCES commerce_order_item(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业订单优惠明细表';

-- 订单状态日志
CREATE TABLE IF NOT EXISTS commerce_order_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  from_status VARCHAR(32) COMMENT '原状态',
  to_status VARCHAR(32) NOT NULL COMMENT '新状态',
  action VARCHAR(32) NOT NULL COMMENT '动作',
  operator_type VARCHAR(16) NOT NULL COMMENT '操作方(USER/ADMIN/SYSTEM)',
  operator_id BIGINT NULL COMMENT '操作人ID',
  remark VARCHAR(255) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_order_time (order_id, created_at),
  CONSTRAINT fk_order_status_log_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业订单状态日志表';

-- 支付记录（无独立支付服务，本表兼作支付单）
CREATE TABLE IF NOT EXISTS commerce_payment_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付记录ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  payment_order_id VARCHAR(64) NOT NULL COMMENT '支付单号',
  pay_channel VARCHAR(24) NOT NULL COMMENT '支付渠道(WALLET/MOCK_WECHAT)',
  pay_amount BIGINT NOT NULL COMMENT '支付金额(分)',
  status VARCHAR(24) NOT NULL DEFAULT 'CREATED' COMMENT '状态(CREATED/PAID/FAILED/CLOSED)',
  idempotency_key VARCHAR(128) NOT NULL COMMENT '支付幂等键',
  external_tx_no VARCHAR(100) COMMENT '外部交易号/钱包流水号',
  paid_time TIMESTAMP NULL COMMENT '支付时间',
  fail_reason VARCHAR(255) COMMENT '失败原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_payment_order (payment_order_id),
  UNIQUE KEY uk_idempotency (idempotency_key),
  KEY idx_order (order_id),
  CONSTRAINT fk_payment_link_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业支付记录表';

-- 库存预占
CREATE TABLE IF NOT EXISTS shop_inventory_reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预占ID',
  reservation_no VARCHAR(64) NOT NULL COMMENT '预占单号',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  order_item_id BIGINT NOT NULL COMMENT '订单项ID',
  sku_id BIGINT NOT NULL COMMENT 'SKU ID',
  qty INT NOT NULL COMMENT '数量',
  status VARCHAR(24) NOT NULL DEFAULT 'RESERVED' COMMENT '状态(RESERVED/CONFIRMED/RELEASED)',
  expire_time TIMESTAMP NULL COMMENT '预占过期时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_reservation_no (reservation_no),
  UNIQUE KEY uk_order_item (order_item_id),
  KEY idx_expire (status, expire_time),
  CONSTRAINT fk_reservation_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_reservation_item FOREIGN KEY (order_item_id) REFERENCES commerce_order_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_reservation_sku FOREIGN KEY (sku_id) REFERENCES shop_sku(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SKU库存预占表';
