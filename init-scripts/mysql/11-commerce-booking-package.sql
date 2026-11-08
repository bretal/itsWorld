-- ========================================
-- 11 商业服务预约与套餐
-- ========================================
-- 预约：时段容量、预约、状态日志。
-- 套餐：模板/固定项/选项/用户套餐/次数/核销流水。
-- 已有数据卷需手动执行一次；新数据卷按 01→11 自动执行。
-- ========================================

USE its_world;

-- 可预约时段
CREATE TABLE IF NOT EXISTS booking_slot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '时段ID',
  store_id BIGINT NOT NULL COMMENT '门店ID',
  staff_id BIGINT NOT NULL DEFAULT 0 COMMENT '员工ID，0表示不限',
  service_date DATE NOT NULL COMMENT '可约日期',
  start_time TIME NOT NULL COMMENT '开始时间',
  end_time TIME NOT NULL COMMENT '结束时间',
  total_capacity INT NOT NULL DEFAULT 1 COMMENT '总容量',
  reserved_capacity INT NOT NULL DEFAULT 0 COMMENT '已预约容量',
  locked_capacity INT NOT NULL DEFAULT 0 COMMENT '锁定容量',
  status VARCHAR(24) NOT NULL DEFAULT 'OPEN' COMMENT '状态 OPEN/CLOSED',
  version INT NOT NULL DEFAULT 0 COMMENT '版本号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_slot (store_id, staff_id, service_date, start_time, end_time),
  KEY idx_date (store_id, service_date),
  CONSTRAINT fk_slot_store FOREIGN KEY (store_id) REFERENCES commerce_store(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可预约时段表';

-- 美容预约
CREATE TABLE IF NOT EXISTS grooming_booking (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约ID',
  booking_no VARCHAR(64) NOT NULL COMMENT '预约号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  pet_id BIGINT NOT NULL COMMENT '宠物ID',
  store_id BIGINT NOT NULL COMMENT '门店ID',
  staff_id BIGINT NULL COMMENT '员工ID',
  slot_id BIGINT NOT NULL COMMENT '时段ID',
  order_id BIGINT NULL COMMENT '关联订单ID(现金支付)',
  service_id BIGINT NOT NULL COMMENT '服务ID',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  reserve_expire_time TIMESTAMP NULL COMMENT '支付保留截止时间',
  checkin_time TIMESTAMP NULL COMMENT '签到时间',
  completed_time TIMESTAMP NULL COMMENT '完成时间',
  cancel_reason VARCHAR(255) COMMENT '取消原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_booking_no (booking_no),
  KEY idx_user_status (user_id, status),
  KEY idx_slot (slot_id),
  CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_booking_pet FOREIGN KEY (pet_id) REFERENCES pet_profile(id),
  CONSTRAINT fk_booking_store FOREIGN KEY (store_id) REFERENCES commerce_store(id),
  CONSTRAINT fk_booking_staff FOREIGN KEY (staff_id) REFERENCES commerce_staff(id) ON DELETE SET NULL,
  CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES booking_slot(id),
  CONSTRAINT fk_booking_order FOREIGN KEY (order_id) REFERENCES commerce_order(id) ON DELETE SET NULL,
  CONSTRAINT fk_booking_service FOREIGN KEY (service_id) REFERENCES grooming_service(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美容预约表';

-- 预约状态日志
CREATE TABLE IF NOT EXISTS grooming_booking_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  booking_id BIGINT NOT NULL COMMENT '预约ID',
  from_status VARCHAR(32) COMMENT '原状态',
  to_status VARCHAR(32) NOT NULL COMMENT '新状态',
  action VARCHAR(32) NOT NULL COMMENT '动作',
  operator_type VARCHAR(16) NOT NULL COMMENT '操作方(USER/ADMIN/SYSTEM)',
  operator_id BIGINT NULL COMMENT '操作人ID',
  remark VARCHAR(255) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_booking_time (booking_id, created_at),
  CONSTRAINT fk_booking_log_booking FOREIGN KEY (booking_id) REFERENCES grooming_booking(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美容预约状态日志表';

-- 套餐模板
CREATE TABLE IF NOT EXISTS service_package_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '套餐模板ID',
  package_code VARCHAR(64) NOT NULL COMMENT '套餐编码',
  name VARCHAR(128) NOT NULL COMMENT '套餐名称',
  cover_url VARCHAR(512) COMMENT '封面图',
  description TEXT COMMENT '套餐说明',
  sale_price BIGINT NOT NULL DEFAULT 0 COMMENT '售价(分)',
  valid_days INT NULL COMMENT '有效天数，NULL为长期',
  status VARCHAR(24) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  allow_cross_store TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许跨店',
  version INT NOT NULL DEFAULT 0 COMMENT '版本号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_package_code (package_code),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务套餐模板表';

-- 套餐固定服务项
CREATE TABLE IF NOT EXISTS service_package_template_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板项ID',
  package_id BIGINT NOT NULL COMMENT '套餐ID',
  service_id BIGINT NOT NULL COMMENT '服务ID',
  included_times INT NOT NULL DEFAULT 1 COMMENT '包含次数',
  selectable_group VARCHAR(32) NULL COMMENT '可选组标识',
  extra_price BIGINT NOT NULL DEFAULT 0 COMMENT '加价(分)',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_package (package_id),
  CONSTRAINT fk_package_item_package FOREIGN KEY (package_id) REFERENCES service_package_template(id) ON DELETE CASCADE,
  CONSTRAINT fk_package_item_service FOREIGN KEY (service_id) REFERENCES grooming_service(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐固定服务项表';

-- 套餐选项组
CREATE TABLE IF NOT EXISTS service_package_option (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '选项组ID',
  package_id BIGINT NOT NULL COMMENT '套餐ID',
  option_code VARCHAR(64) NOT NULL COMMENT '选项编码',
  option_name VARCHAR(128) NOT NULL COMMENT '选项名称',
  min_select INT NOT NULL DEFAULT 0 COMMENT '最少选择',
  max_select INT NOT NULL DEFAULT 1 COMMENT '最多选择',
  required TINYINT NOT NULL DEFAULT 0 COMMENT '是否必选',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_package_option (package_id, option_code),
  CONSTRAINT fk_package_option_package FOREIGN KEY (package_id) REFERENCES service_package_template(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐选项组表';

-- 套餐选项候选服务
CREATE TABLE IF NOT EXISTS service_package_option_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '选项候选ID',
  option_id BIGINT NOT NULL COMMENT '选项组ID',
  service_id BIGINT NOT NULL COMMENT '服务ID',
  add_price BIGINT NOT NULL DEFAULT 0 COMMENT '加价(分)',
  included_times INT NOT NULL DEFAULT 1 COMMENT '包含次数',
  status VARCHAR(24) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_option_service (option_id, service_id),
  CONSTRAINT fk_option_item_option FOREIGN KEY (option_id) REFERENCES service_package_option(id) ON DELETE CASCADE,
  CONSTRAINT fk_option_item_service FOREIGN KEY (service_id) REFERENCES grooming_service(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐选项候选服务表';

-- 用户已购套餐
CREATE TABLE IF NOT EXISTS user_service_package (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户套餐ID',
  package_instance_no VARCHAR(64) NOT NULL COMMENT '套餐实例号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  pet_id BIGINT NULL COMMENT '绑定宠物ID',
  package_id BIGINT NOT NULL COMMENT '套餐模板ID',
  order_item_id BIGINT NOT NULL COMMENT '订单项ID',
  store_id BIGINT NULL COMMENT '购买门店ID',
  name_snapshot VARCHAR(128) NOT NULL COMMENT '套餐名称快照',
  selection_snapshot JSON NULL COMMENT '选项快照',
  start_time TIMESTAMP NOT NULL COMMENT '生效时间',
  expire_time TIMESTAMP NULL COMMENT '过期时间',
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/USED_UP/EXPIRED/CANCELLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_instance_no (package_instance_no),
  UNIQUE KEY uk_order_item (order_item_id),
  KEY idx_user_status (user_id, status),
  CONSTRAINT fk_user_package_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_package_pet FOREIGN KEY (pet_id) REFERENCES pet_profile(id) ON DELETE SET NULL,
  CONSTRAINT fk_user_package_template FOREIGN KEY (package_id) REFERENCES service_package_template(id),
  CONSTRAINT fk_user_package_order_item FOREIGN KEY (order_item_id) REFERENCES commerce_order_item(id),
  CONSTRAINT fk_user_package_store FOREIGN KEY (store_id) REFERENCES commerce_store(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户已购套餐表';

-- 用户套餐服务次数
CREATE TABLE IF NOT EXISTS user_service_package_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '套餐服务项ID',
  user_package_id BIGINT NOT NULL COMMENT '用户套餐ID',
  service_id BIGINT NOT NULL COMMENT '服务ID',
  total_times INT NOT NULL DEFAULT 0 COMMENT '总次数',
  used_times INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
  frozen_times INT NOT NULL DEFAULT 0 COMMENT '冻结次数',
  service_snapshot JSON NULL COMMENT '服务快照',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_instance_service (user_package_id, service_id),
  CONSTRAINT fk_user_package_item_package FOREIGN KEY (user_package_id) REFERENCES user_service_package(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_package_item_service FOREIGN KEY (service_id) REFERENCES grooming_service(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户套餐服务次数表';

-- 套餐使用/核销流水
CREATE TABLE IF NOT EXISTS service_package_usage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '核销ID',
  usage_no VARCHAR(64) NOT NULL COMMENT '核销单号',
  user_package_id BIGINT NOT NULL COMMENT '用户套餐ID',
  package_item_id BIGINT NOT NULL COMMENT '套餐服务项ID',
  booking_id BIGINT NULL COMMENT '预约ID',
  service_id BIGINT NOT NULL COMMENT '服务ID',
  qty INT NOT NULL DEFAULT 1 COMMENT '次数',
  status VARCHAR(24) NOT NULL DEFAULT 'FROZEN' COMMENT '状态 FROZEN/USED/RELEASED/REVERSED',
  operator_id BIGINT NULL COMMENT '操作人ID',
  used_time TIMESTAMP NULL COMMENT '核销时间',
  idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_usage_no (usage_no),
  UNIQUE KEY uk_idempotency (idempotency_key),
  KEY idx_booking (booking_id),
  CONSTRAINT fk_usage_package FOREIGN KEY (user_package_id) REFERENCES user_service_package(id) ON DELETE CASCADE,
  CONSTRAINT fk_usage_package_item FOREIGN KEY (package_item_id) REFERENCES user_service_package_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_usage_booking FOREIGN KEY (booking_id) REFERENCES grooming_booking(id) ON DELETE SET NULL,
  CONSTRAINT fk_usage_service FOREIGN KEY (service_id) REFERENCES grooming_service(id),
  CONSTRAINT fk_usage_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐使用核销流水表';
