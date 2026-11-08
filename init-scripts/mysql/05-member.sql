-- ========================================
-- 05 会员域（等级 / 成长 / 积分 / 任务 / 券 / 礼包）
-- ========================================
-- 金额统一使用 BIGINT 整数分；会员流水追加式，不做物理删除。
-- 本脚本只对空数据卷自动执行一次；已有数据卷需手动执行一次。
-- ========================================

USE its_world;

-- 会员等级
CREATE TABLE IF NOT EXISTS member_level (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '等级ID',
  level_code VARCHAR(30) NOT NULL COMMENT '等级编码',
  level_name VARCHAR(50) NOT NULL COMMENT '等级名称',
  growth_threshold INT NOT NULL DEFAULT 0 COMMENT '成长值门槛',
  icon_url VARCHAR(500) COMMENT '等级图标URL',
  discount_rate DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '会员折扣率(0-1)',
  description VARCHAR(255) COMMENT '等级说明',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1启用 0停用)',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_level_code (level_code),
  UNIQUE KEY uk_growth_threshold (growth_threshold),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级表';

-- 会员权益定义
CREATE TABLE IF NOT EXISTS member_benefit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权益ID',
  benefit_name VARCHAR(50) NOT NULL COMMENT '权益名称',
  benefit_type VARCHAR(30) COMMENT '权益类型(POINTS/DISCOUNT/GIFT/SERVICE/OTHER)',
  benefit_value VARCHAR(50) COMMENT '权益值',
  description VARCHAR(255) COMMENT '权益说明',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_benefit_name (benefit_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员权益定义表';

-- 等级与权益映射
CREATE TABLE IF NOT EXISTS member_level_benefit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '映射ID',
  level_id BIGINT NOT NULL COMMENT '等级ID',
  benefit_id BIGINT NOT NULL COMMENT '权益ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_level_benefit (level_id, benefit_id),
  KEY idx_benefit_id (benefit_id),
  CONSTRAINT fk_member_level_benefit_level FOREIGN KEY (level_id) REFERENCES member_level(id) ON DELETE CASCADE,
  CONSTRAINT fk_member_level_benefit_benefit FOREIGN KEY (benefit_id) REFERENCES member_benefit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级权益映射表';

-- 会员账户
CREATE TABLE IF NOT EXISTS member_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会员账户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  level_id BIGINT COMMENT '当前等级ID',
  growth_value INT NOT NULL DEFAULT 0 COMMENT '成长值',
  points_balance INT NOT NULL DEFAULT 0 COMMENT '积分余额',
  version INT NOT NULL DEFAULT 0 COMMENT '版本号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_user_id (user_id),
  KEY idx_level_id (level_id),
  CONSTRAINT fk_member_account_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_member_account_level FOREIGN KEY (level_id) REFERENCES member_level(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员账户表';

-- 等级变更历史
CREATE TABLE IF NOT EXISTS member_level_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  from_level_id BIGINT COMMENT '原等级ID',
  to_level_id BIGINT COMMENT '新等级ID',
  growth_value INT NOT NULL DEFAULT 0 COMMENT '变更时成长值',
  reason VARCHAR(255) COMMENT '变更原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  KEY idx_user_id (user_id),
  KEY idx_created_at (created_at),
  CONSTRAINT fk_member_level_history_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_member_level_history_from FOREIGN KEY (from_level_id) REFERENCES member_level(id) ON DELETE SET NULL,
  CONSTRAINT fk_member_level_history_to FOREIGN KEY (to_level_id) REFERENCES member_level(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级变更历史表';

-- 成长值流水
CREATE TABLE IF NOT EXISTS member_growth_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  change_value INT NOT NULL COMMENT '变动值',
  balance_after INT NOT NULL COMMENT '变动后成长值',
  source_type VARCHAR(50) NOT NULL COMMENT '来源类型(TASK/GIFT/MANUAL)',
  source_no VARCHAR(64) NOT NULL COMMENT '来源单号(幂等)',
  remark VARCHAR(255) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_growth_source (user_id, source_type, source_no),
  KEY idx_user_created (user_id, created_at),
  CONSTRAINT fk_member_growth_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员成长值流水表';

-- 积分流水
CREATE TABLE IF NOT EXISTS member_points_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  change_value INT NOT NULL COMMENT '变动值',
  balance_after INT NOT NULL COMMENT '变动后积分',
  source_type VARCHAR(50) NOT NULL COMMENT '来源类型(TASK/GIFT/MANUAL)',
  source_no VARCHAR(64) NOT NULL COMMENT '来源单号(幂等)',
  remark VARCHAR(255) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_points_source (user_id, source_type, source_no),
  KEY idx_user_created (user_id, created_at),
  CONSTRAINT fk_member_points_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员积分流水表';

-- 会员任务
CREATE TABLE IF NOT EXISTS member_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  task_code VARCHAR(50) NOT NULL COMMENT '任务编码(埋点标识)',
  task_name VARCHAR(50) NOT NULL COMMENT '任务名称',
  description VARCHAR(255) COMMENT '任务说明',
  period_type VARCHAR(20) NOT NULL DEFAULT 'DAILY' COMMENT '周期(DAILY/WEEKLY/ONCE)',
  target_value INT NOT NULL DEFAULT 1 COMMENT '目标次数',
  points_reward INT NOT NULL DEFAULT 0 COMMENT '积分奖励',
  growth_reward INT NOT NULL DEFAULT 0 COMMENT '成长值奖励',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1启用 0停用)',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_task_code (task_code),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员任务表';

-- 会员任务进度
CREATE TABLE IF NOT EXISTS member_task_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  task_id BIGINT NOT NULL COMMENT '任务ID',
  period_key VARCHAR(20) NOT NULL COMMENT '周期键(日期/周/ONCE)',
  progress INT NOT NULL DEFAULT 0 COMMENT '当前进度',
  reward_status TINYINT NOT NULL DEFAULT 0 COMMENT '奖励状态(0未领取 1已领取)',
  claimed_at TIMESTAMP NULL COMMENT '领取时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  UNIQUE KEY uk_user_task_period (user_id, task_id, period_key),
  KEY idx_user_id (user_id),
  CONSTRAINT fk_member_task_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_member_task_record_task FOREIGN KEY (task_id) REFERENCES member_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员任务进度表';

-- 优惠券模板
CREATE TABLE IF NOT EXISTS coupon_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
  template_name VARCHAR(50) NOT NULL COMMENT '券名称',
  coupon_type VARCHAR(20) NOT NULL DEFAULT 'CASH' COMMENT '券类型(CASH/DISCOUNT)',
  amount_cents BIGINT COMMENT '面额(分)，CASH时使用',
  discount_rate DECIMAL(5,2) COMMENT '折扣率(0-1)，DISCOUNT时使用',
  min_amount_cents BIGINT NOT NULL DEFAULT 0 COMMENT '最低消费(分)',
  total_count INT NOT NULL DEFAULT 0 COMMENT '发行总量',
  issued_count INT NOT NULL DEFAULT 0 COMMENT '已发行数量',
  valid_days INT COMMENT '领取后有效天数',
  description VARCHAR(255) COMMENT '使用说明',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1启用 0停用)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- 用户优惠券
CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户券ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  template_id BIGINT NOT NULL COMMENT '券模板ID',
  coupon_code VARCHAR(40) NOT NULL COMMENT '券码',
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态(AVAILABLE/LOCKED/USED/EXPIRED)',
  valid_from TIMESTAMP NOT NULL COMMENT '生效时间',
  valid_to TIMESTAMP NULL COMMENT '失效时间',
  used_at TIMESTAMP NULL COMMENT '使用时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',

  UNIQUE KEY uk_coupon_code (coupon_code),
  KEY idx_user_status (user_id, status),
  KEY idx_valid_to (valid_to),
  CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_coupon_template FOREIGN KEY (template_id) REFERENCES coupon_template(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- 会员礼包
CREATE TABLE IF NOT EXISTS member_gift (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '礼包ID',
  gift_name VARCHAR(50) NOT NULL COMMENT '礼包名称',
  description VARCHAR(255) COMMENT '礼包说明',
  required_level_id BIGINT COMMENT '领取所需等级ID',
  points_reward INT NOT NULL DEFAULT 0 COMMENT '积分奖励',
  coupon_template_id BIGINT COMMENT '附带券模板ID',
  start_at TIMESTAMP NULL COMMENT '开始时间',
  end_at TIMESTAMP NULL COMMENT '结束时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1启用 0停用)',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  KEY idx_status (status),
  KEY idx_required_level (required_level_id),
  CONSTRAINT fk_member_gift_level FOREIGN KEY (required_level_id) REFERENCES member_level(id) ON DELETE SET NULL,
  CONSTRAINT fk_member_gift_coupon FOREIGN KEY (coupon_template_id) REFERENCES coupon_template(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员礼包表';

-- 礼包领取记录
CREATE TABLE IF NOT EXISTS member_gift_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '领取记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  gift_id BIGINT NOT NULL COMMENT '礼包ID',
  points_amount INT NOT NULL DEFAULT 0 COMMENT '发放积分',
  coupon_id BIGINT COMMENT '发放的用户券ID',
  status VARCHAR(20) NOT NULL DEFAULT 'GRANTED' COMMENT '发放状态(GRANTED/FAILED)',
  claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  UNIQUE KEY uk_user_gift (user_id, gift_id),
  KEY idx_user_id (user_id),
  CONSTRAINT fk_member_gift_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_member_gift_record_gift FOREIGN KEY (gift_id) REFERENCES member_gift(id) ON DELETE CASCADE,
  CONSTRAINT fk_member_gift_record_coupon FOREIGN KEY (coupon_id) REFERENCES user_coupon(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员礼包领取记录表';

-- ========================================
-- 种子数据（显式 ID，INSERT IGNORE 便于重复执行）
-- ========================================

INSERT IGNORE INTO member_level
(id, level_code, level_name, growth_threshold, icon_url, discount_rate, description, status, sort_order, created_at, updated_at)
VALUES
(1, 'NORMAL', '普通会员', 0, NULL, 1.00, '注册即享基础权益', 1, 1, NOW(), NOW()),
(2, 'SILVER', '银卡会员', 1000, NULL, 0.98, '成长值满 1000 解锁', 1, 2, NOW(), NOW()),
(3, 'GOLD', '金卡会员', 3000, NULL, 0.95, '成长值满 3000 解锁', 1, 3, NOW(), NOW()),
(4, 'DIAMOND', '钻石会员', 10000, NULL, 0.90, '成长值满 10000 解锁', 1, 4, NOW(), NOW());

INSERT IGNORE INTO member_benefit
(id, benefit_name, benefit_type, benefit_value, description, created_at)
VALUES
(1, '积分累积', 'POINTS', '1x', '消费与任务可获得积分', NOW()),
(2, '生日礼包', 'GIFT', '每年1次', '生日当月可领取专属礼包', NOW()),
(3, '美容98折', 'DISCOUNT', '0.98', '美容服务享 98 折', NOW()),
(4, '优先预约', 'SERVICE', '优先', '门店服务优先预约', NOW()),
(5, '美容95折', 'DISCOUNT', '0.95', '美容服务享 95 折', NOW()),
(6, '免费体检', 'SERVICE', '每年1次', '每年 1 次基础体检', NOW()),
(7, '专属客服', 'SERVICE', '7×12', '专属客服支持', NOW()),
(8, '美容9折', 'DISCOUNT', '0.90', '美容服务享 9 折', NOW());

INSERT IGNORE INTO member_level_benefit (id, level_id, benefit_id, created_at)
VALUES
(1, 1, 1, NOW()),
(2, 2, 1, NOW()),
(3, 2, 2, NOW()),
(4, 2, 3, NOW()),
(5, 3, 1, NOW()),
(6, 3, 2, NOW()),
(7, 3, 5, NOW()),
(8, 3, 4, NOW()),
(9, 4, 1, NOW()),
(10, 4, 2, NOW()),
(11, 4, 8, NOW()),
(12, 4, 6, NOW()),
(13, 4, 7, NOW());

INSERT IGNORE INTO member_task
(id, task_code, task_name, description, period_type, target_value, points_reward, growth_reward, status, sort_order, created_at, updated_at)
VALUES
(1, 'DAILY_CHECKIN', '每日签到', '每天签到一次，保持活跃', 'DAILY', 1, 5, 5, 1, 1, NOW(), NOW()),
(2, 'DIARY_CREATE', '记录日记', '每天记录一篇宠物日记', 'DAILY', 1, 10, 10, 1, 2, NOW(), NOW()),
(3, 'POST_CREATE', '社区发帖', '每天在宠友圈发布一篇帖子', 'DAILY', 1, 10, 5, 1, 3, NOW(), NOW()),
(4, 'PET_CREATE', '完善宠物档案', '首次创建宠物档案', 'ONCE', 1, 100, 1000, 1, 4, NOW(), NOW());

INSERT IGNORE INTO coupon_template
(id, template_name, coupon_type, amount_cents, discount_rate, min_amount_cents, total_count, issued_count, valid_days, description, status, created_at, updated_at)
VALUES
(1, '新人5元券', 'CASH', 500, NULL, 5000, 1000, 0, 30, '满 50 元可用', 1, NOW(), NOW()),
(2, '满100减10', 'CASH', 1000, NULL, 10000, 500, 0, 30, '满 100 元可用', 1, NOW(), NOW()),
(3, '美容9折券', 'DISCOUNT', NULL, 0.90, 0, 300, 0, 60, '美容服务享 9 折', 1, NOW(), NOW());

INSERT IGNORE INTO member_gift
(id, gift_name, description, required_level_id, points_reward, coupon_template_id, start_at, end_at, status, sort_order, created_at, updated_at)
VALUES
(1, '新人礼包', '注册即可领取的见面礼', 1, 50, 1, NULL, NULL, 1, 1, NOW(), NOW()),
(2, '银卡生日礼包', '银卡及以上会员生日礼包', 2, 100, 2, NULL, NULL, 1, 2, NOW(), NOW());
