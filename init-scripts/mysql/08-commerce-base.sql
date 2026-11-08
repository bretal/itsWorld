-- ========================================
-- 08 商业服务基础资料（门店 / 美容 / 商品 / 库存）
-- ========================================
-- Phase 1：不含订单、支付、预约、套餐、售后、评价。
-- 金额统一 BIGINT 整数分；状态用 VARCHAR；配置类下架用 status，不物理删除。
-- 已有数据卷需手动执行一次；新数据卷按 01→08 自动执行。
-- ========================================

USE its_world;

-- 门店
CREATE TABLE IF NOT EXISTS commerce_store (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '门店ID',
  store_code VARCHAR(64) NOT NULL COMMENT '门店编码',
  name VARCHAR(128) NOT NULL COMMENT '门店名称',
  phone VARCHAR(32) COMMENT '联系电话',
  province VARCHAR(64) COMMENT '省',
  city VARCHAR(64) COMMENT '市',
  district VARCHAR(64) COMMENT '区',
  address VARCHAR(255) COMMENT '详细地址',
  longitude DECIMAL(10,6) COMMENT '经度',
  latitude DECIMAL(10,6) COMMENT '纬度',
  business_status VARCHAR(24) NOT NULL DEFAULT 'OPEN' COMMENT '营业状态 OPEN/CLOSED/SUSPENDED',
  description TEXT COMMENT '门店说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_store_code (store_code),
  KEY idx_business_status (business_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商业门店表';

-- 门店营业时间/例外日
CREATE TABLE IF NOT EXISTS commerce_store_business_hour (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '营业时间ID',
  store_id BIGINT NOT NULL COMMENT '门店ID',
  day_of_week TINYINT NOT NULL COMMENT '星期(1-7)',
  open_time TIME NOT NULL COMMENT '开始时间',
  close_time TIME NOT NULL COMMENT '结束时间',
  special_date DATE NULL COMMENT '特殊日期，NULL表示每周规则',
  closed TINYINT NOT NULL DEFAULT 0 COMMENT '是否停业(1停业 0营业)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_store_day (store_id, day_of_week, special_date),
  CONSTRAINT fk_store_hour_store FOREIGN KEY (store_id) REFERENCES commerce_store(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店营业时间表';

-- 门店员工
CREATE TABLE IF NOT EXISTS commerce_staff (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
  store_id BIGINT NOT NULL COMMENT '门店ID',
  staff_name VARCHAR(64) NOT NULL COMMENT '员工姓名',
  mobile VARCHAR(32) COMMENT '手机号',
  role_code VARCHAR(32) COMMENT '角色编码(GROOMER等)',
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/INACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_store_status (store_id, status),
  CONSTRAINT fk_staff_store FOREIGN KEY (store_id) REFERENCES commerce_store(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店员工表';

-- 员工班次
CREATE TABLE IF NOT EXISTS commerce_staff_shift (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班次ID',
  staff_id BIGINT NOT NULL COMMENT '员工ID',
  store_id BIGINT NOT NULL COMMENT '门店ID',
  shift_date DATE NOT NULL COMMENT '班次日期',
  start_time TIME NOT NULL COMMENT '开始时间',
  end_time TIME NOT NULL COMMENT '结束时间',
  capacity INT NOT NULL DEFAULT 1 COMMENT '班次容量',
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/CLOSED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_staff_date (staff_id, shift_date),
  KEY idx_store_date (store_id, shift_date),
  CONSTRAINT fk_shift_staff FOREIGN KEY (staff_id) REFERENCES commerce_staff(id) ON DELETE CASCADE,
  CONSTRAINT fk_shift_store FOREIGN KEY (store_id) REFERENCES commerce_store(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工班次表';

-- 美容分类
CREATE TABLE IF NOT EXISTS grooming_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '美容分类ID',
  name VARCHAR(64) NOT NULL COMMENT '分类名称',
  icon_url VARCHAR(512) COMMENT '分类图标',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  status VARCHAR(24) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_status_sort (status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美容服务分类表';

-- 美容服务项目
CREATE TABLE IF NOT EXISTS grooming_service (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '服务ID',
  category_id BIGINT COMMENT '分类ID',
  service_code VARCHAR(64) NOT NULL COMMENT '服务编码',
  name VARCHAR(128) NOT NULL COMMENT '服务名称',
  cover_url VARCHAR(512) COMMENT '封面图',
  description TEXT COMMENT '服务说明',
  duration_min INT NOT NULL DEFAULT 0 COMMENT '服务时长(分钟)',
  base_price BIGINT NOT NULL DEFAULT 0 COMMENT '基础价(分)',
  status VARCHAR(24) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_service_code (service_code),
  KEY idx_category_status (category_id, status),
  CONSTRAINT fk_grooming_service_category FOREIGN KEY (category_id) REFERENCES grooming_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美容服务项目表';

-- 门店服务与差异价
CREATE TABLE IF NOT EXISTS grooming_service_store (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '门店服务ID',
  service_id BIGINT NOT NULL COMMENT '服务ID',
  store_id BIGINT NOT NULL COMMENT '门店ID',
  sale_price BIGINT NOT NULL DEFAULT 0 COMMENT '门店售价(分)',
  capacity_per_slot INT NOT NULL DEFAULT 1 COMMENT '每时段容量',
  status VARCHAR(24) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_service_store (service_id, store_id),
  KEY idx_store_status (store_id, status),
  CONSTRAINT fk_service_store_service FOREIGN KEY (service_id) REFERENCES grooming_service(id) ON DELETE CASCADE,
  CONSTRAINT fk_service_store_store FOREIGN KEY (store_id) REFERENCES commerce_store(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店美容服务与价格表';

-- 服务适配宠物规则
CREATE TABLE IF NOT EXISTS grooming_service_pet_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
  service_id BIGINT NOT NULL COMMENT '服务ID',
  species_code VARCHAR(32) NOT NULL COMMENT '宠物类型(dog/cat/...)',
  min_weight_gram INT NULL COMMENT '最低体重(克)',
  max_weight_gram INT NULL COMMENT '最高体重(克)',
  min_age_month INT NULL COMMENT '最低月龄',
  max_age_month INT NULL COMMENT '最高月龄',
  note VARCHAR(255) COMMENT '说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_service (service_id),
  CONSTRAINT fk_pet_rule_service FOREIGN KEY (service_id) REFERENCES grooming_service(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美容服务宠物适配规则表';

-- 商品分类
CREATE TABLE IF NOT EXISTS shop_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品分类ID',
  parent_id BIGINT NULL COMMENT '父分类ID',
  name VARCHAR(64) NOT NULL COMMENT '分类名称',
  icon_url VARCHAR(512) COMMENT '分类图标',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  status VARCHAR(24) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_parent_sort (parent_id, sort),
  CONSTRAINT fk_shop_category_parent FOREIGN KEY (parent_id) REFERENCES shop_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 商品 SPU
CREATE TABLE IF NOT EXISTS shop_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
  product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
  category_id BIGINT NULL COMMENT '分类ID',
  name VARCHAR(160) NOT NULL COMMENT '商品名称',
  subtitle VARCHAR(255) COMMENT '副标题',
  description LONGTEXT COMMENT '商品详情',
  cover_url VARCHAR(512) COMMENT '封面图',
  sale_status VARCHAR(24) NOT NULL DEFAULT 'OFF_SALE' COMMENT '销售状态 ON_SALE/OFF_SALE',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_product_code (product_code),
  KEY idx_category_status (category_id, sale_status),
  CONSTRAINT fk_shop_product_category FOREIGN KEY (category_id) REFERENCES shop_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SPU表';

-- 商品轮播图
CREATE TABLE IF NOT EXISTS shop_product_image (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图片ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  image_url VARCHAR(512) NOT NULL COMMENT '图片URL',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_product_sort (product_id, sort),
  CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES shop_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品轮播图表';

-- 商品 SKU
CREATE TABLE IF NOT EXISTS shop_sku (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'SKU ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  sku_code VARCHAR(64) NOT NULL COMMENT 'SKU编码',
  spec_json JSON NULL COMMENT '规格JSON',
  sale_price BIGINT NOT NULL DEFAULT 0 COMMENT '销售价(分)',
  market_price BIGINT NOT NULL DEFAULT 0 COMMENT '市场价(分)',
  weight_gram INT NULL COMMENT '重量(克)',
  status VARCHAR(24) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  version INT NOT NULL DEFAULT 0 COMMENT '版本号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_sku_code (sku_code),
  KEY idx_product_status (product_id, status),
  CONSTRAINT fk_sku_product FOREIGN KEY (product_id) REFERENCES shop_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- SKU 库存
CREATE TABLE IF NOT EXISTS shop_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID',
  sku_id BIGINT NOT NULL COMMENT 'SKU ID',
  warehouse_id BIGINT NOT NULL DEFAULT 0 COMMENT '仓库ID，0为默认单仓',
  available_qty INT NOT NULL DEFAULT 0 COMMENT '可售库存',
  locked_qty INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
  sold_qty INT NOT NULL DEFAULT 0 COMMENT '已售库存',
  version INT NOT NULL DEFAULT 0 COMMENT '版本号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_sku_warehouse (sku_id, warehouse_id),
  CONSTRAINT fk_inventory_sku FOREIGN KEY (sku_id) REFERENCES shop_sku(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SKU库存表';

-- 库存变动流水
CREATE TABLE IF NOT EXISTS shop_inventory_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存流水ID',
  sku_id BIGINT NOT NULL COMMENT 'SKU ID',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型(ADMIN_ADJUST/ORDER_RESERVE/...)',
  biz_id BIGINT NULL COMMENT '业务ID',
  change_available INT NOT NULL DEFAULT 0 COMMENT '可售变动',
  change_locked INT NOT NULL DEFAULT 0 COMMENT '锁定变动',
  before_available INT NOT NULL DEFAULT 0 COMMENT '变动前可售',
  after_available INT NOT NULL DEFAULT 0 COMMENT '变动后可售',
  idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
  remark VARCHAR(255) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_idempotency (idempotency_key),
  KEY idx_sku_time (sku_id, created_at),
  CONSTRAINT fk_inventory_log_sku FOREIGN KEY (sku_id) REFERENCES shop_sku(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存变动流水表';
