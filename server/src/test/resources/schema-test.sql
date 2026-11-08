-- ========================================
-- ItsWorld 数据库初始化脚本
-- ========================================
-- 创建日期: 2026-10-30
-- 数据库: its_world
-- 字符集: utf8mb4 (支持emoji)
-- ========================================

USE its_world;

-- ========================================
-- 1. 用户认证与个人中心模块 (com.itsworld.user)
-- ========================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名（登录账号）',
  email VARCHAR(100) UNIQUE COMMENT '邮箱',
  phone VARCHAR(20) UNIQUE COMMENT '手机号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希（bcrypt）',
  password_salt VARCHAR(64) COMMENT '密码盐值（预留）',
  nick_name VARCHAR(50) COMMENT '昵称',
  avatar_url VARCHAR(500) COMMENT '头像URL（MinIO)',
  bio TEXT COMMENT '个人简介',
  gender TINYINT COMMENT '性别 (0-未设置 1-男 2-女)',
  birth_date DATE COMMENT '出生日期',
  region VARCHAR(100) COMMENT '地区',
  status TINYINT DEFAULT 1 COMMENT '账户状态 (1-正常 0-禁用)',
  last_login_at TIMESTAMP COMMENT '最后登录时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_at TIMESTAMP NULL COMMENT '软删除时间',

  KEY idx_username (username),
  KEY idx_email (email),
  KEY idx_phone (phone),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户token黑名单（存Redis效率更高，此处作备选）
CREATE TABLE IF NOT EXISTS user_token_blacklist (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  token VARCHAR(500) NOT NULL COMMENT 'JWT token',
  expired_at TIMESTAMP NOT NULL COMMENT 'token过期时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入黑名单时间',

  KEY idx_user_id (user_id),
  KEY idx_expired_at (expired_at),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JWT Token 黑名单';

-- 用户消息表（待设计）
CREATE TABLE IF NOT EXISTS user_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
  user_id BIGINT NOT NULL COMMENT '接收者ID',
  sender_id BIGINT COMMENT '发送者ID（系统消息为NULL）',
  message_type VARCHAR(50) COMMENT '消息类型 (like,comment,follow,system)',
  title VARCHAR(100) COMMENT '消息标题',
  content TEXT COMMENT '消息内容',
  is_read TINYINT DEFAULT 0 COMMENT '是否已读',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  KEY idx_user_id (user_id),
  KEY idx_created_at (created_at),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户消息表';

-- ========================================
-- 2. 宠物档案模块 (com.itsworld.pet)
-- ========================================

-- 宠物档案表
CREATE TABLE IF NOT EXISTS pet_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '宠物ID',
  user_id BIGINT NOT NULL COMMENT '主人ID',
  pet_name VARCHAR(50) NOT NULL COMMENT '宠物名称',
  pet_type VARCHAR(20) COMMENT '宠物类型 (dog,cat,rabbit,bird...)',
  breed VARCHAR(50) COMMENT '品种',
  birth_date DATE COMMENT '出生日期',
  gender TINYINT COMMENT '性别 (0-未知 1-公 2-母)',
  weight_kg DECIMAL(5,2) COMMENT '当前体重(kg)',
  height_cm DECIMAL(5,2) COMMENT '身高(cm)',
  avatar_url VARCHAR(500) COMMENT '宠物头像(MinIO)',
  bio TEXT COMMENT '宠物简介',
  is_active TINYINT DEFAULT 1 COMMENT '是否为活跃宠物(用户正在记录的)',
  care_notes TEXT COMMENT '饲养笔记',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,

  KEY idx_user_id (user_id),
  KEY idx_is_active (is_active),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物档案表';

-- 宠物体重历史表（健康管理）
CREATE TABLE IF NOT EXISTS pet_weight_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  pet_id BIGINT NOT NULL COMMENT '宠物ID',
  weight_kg DECIMAL(5,2) NOT NULL COMMENT '体重(kg)',
  recorded_at TIMESTAMP NOT NULL COMMENT '记录日期',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  KEY idx_pet_id (pet_id),
  KEY idx_recorded_at (recorded_at),
  FOREIGN KEY (pet_id) REFERENCES pet_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物体重历史表';

-- 宠物喂食记录表
CREATE TABLE IF NOT EXISTS pet_feeding_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  pet_id BIGINT NOT NULL COMMENT '宠物ID',
  feed_type VARCHAR(50) COMMENT '饲料类型 (dry_food,wet_food,treats...)',
  amount_g DECIMAL(8,2) COMMENT '喂食量(克)',
  meal_time TIMESTAMP NOT NULL COMMENT '喂食时间',
  notes TEXT COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  KEY idx_pet_id (pet_id),
  KEY idx_meal_time (meal_time),
  FOREIGN KEY (pet_id) REFERENCES pet_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物喂食记录表';

-- 宠物疫苗接种记录表
CREATE TABLE IF NOT EXISTS pet_vaccine_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  pet_id BIGINT NOT NULL COMMENT '宠物ID',
  vaccine_name VARCHAR(100) COMMENT '疫苗名称',
  vaccinated_at DATE COMMENT '接种日期',
  next_due_at DATE COMMENT '下次接种日期',
  vet_clinic VARCHAR(100) COMMENT '接种医院',
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  KEY idx_pet_id (pet_id),
  KEY idx_next_due_at (next_due_at),
  FOREIGN KEY (pet_id) REFERENCES pet_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物疫苗记录表';

-- 宠物体检记录表
CREATE TABLE IF NOT EXISTS pet_health_checkup (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  pet_id BIGINT NOT NULL COMMENT '宠物ID',
  checkup_date DATE COMMENT '体检日期',
  vet_clinic VARCHAR(100) COMMENT '体检医院',
  findings TEXT COMMENT '体检发现',
  recommendations TEXT COMMENT '医生建议',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  KEY idx_pet_id (pet_id),
  KEY idx_checkup_date (checkup_date),
  FOREIGN KEY (pet_id) REFERENCES pet_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物体检记录表';

-- ========================================
-- 3. 每日唤醒与日记模块 (com.itsworld.diary)
-- ========================================

-- 宠物日记表
CREATE TABLE IF NOT EXISTS pet_diary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日记ID',
  pet_id BIGINT NOT NULL COMMENT '宠物ID',
  user_id BIGINT NOT NULL COMMENT '主人ID',
  diary_date DATE NOT NULL COMMENT '日记日期',
  category VARCHAR(20) COMMENT '日记分类 (daily-日常, food-饮食, health-健康)',

  -- 6步唤醒问候数据
  morning_mood VARCHAR(50) COMMENT '早晨心情 (happy,sad,neutral...)',
  question_1 VARCHAR(200) COMMENT '第1个问题答案：今天的天气？',
  question_2 VARCHAR(200) COMMENT '第2个问题答案：今天的目标？',
  question_3 VARCHAR(200) COMMENT '第3个问题答案：今天有什么特别？',
  question_4 VARCHAR(200) COMMENT '第4个问题答案：宠物今天表现？',
  question_5 VARCHAR(200) COMMENT '第5个问题答案：你的感受？',
  question_6 VARCHAR(200) COMMENT '第6个问题答案：一句话总结？',

  -- 日记内容
  content TEXT COMMENT '完整日记内容',
  images_json JSON COMMENT '日记图片URL数组(MinIO存储路径)',

  -- 状态与互动
  is_published TINYINT DEFAULT 1 COMMENT '是否发布（0-草稿 1-已发布）',
  like_count INT DEFAULT 0 COMMENT '点赞数',
  comment_count INT DEFAULT 0 COMMENT '评论数',
  view_count INT DEFAULT 0 COMMENT '浏览数',

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_pet_date (pet_id, diary_date),
  KEY idx_user_id (user_id),
  KEY idx_is_published (is_published),
  KEY idx_diary_date (diary_date),
  FOREIGN KEY (pet_id) REFERENCES pet_profile(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物日记表';

-- 日记点赞表
CREATE TABLE IF NOT EXISTS diary_like (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
  diary_id BIGINT NOT NULL COMMENT '日记ID',
  user_id BIGINT NOT NULL COMMENT '点赞用户ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_diary_user (diary_id, user_id),
  FOREIGN KEY (diary_id) REFERENCES pet_diary(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记点赞表';

-- 日记评论表
CREATE TABLE IF NOT EXISTS diary_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  diary_id BIGINT NOT NULL COMMENT '日记ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  content VARCHAR(500) COMMENT '评论内容',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  KEY idx_diary_id (diary_id),
  KEY idx_user_id (user_id),
  FOREIGN KEY (diary_id) REFERENCES pet_diary(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记评论表';

-- ========================================
-- 4. AI 情绪识别模块 (com.itsworld.ai)
-- ========================================

-- AI情绪识别结果表
CREATE TABLE IF NOT EXISTS ai_emotion_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '识别ID',
  pet_id BIGINT NOT NULL COMMENT '宠物ID',
  image_url VARCHAR(500) COMMENT '原始图片URL(MinIO)',

  -- 识别结果
  primary_emotion VARCHAR(50) COMMENT '主要情绪 (happy,sad,angry,calm,scared...)',
  emotion_score DECIMAL(5,2) COMMENT '情绪置信度 (0-100)',
  emotions_json JSON COMMENT '所有情绪分数: {happy:80, sad:10, ...}',

  -- 身体语言解读
  body_language_analysis TEXT COMMENT '身体语言分析结果',
  health_indicators TEXT COMMENT '健康指标 (毛发状态、眼睛、姿态等)',

  -- 建议
  ai_suggestions TEXT COMMENT 'AI建议（互动、护理等）',

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  KEY idx_pet_id (pet_id),
  KEY idx_created_at (created_at),
  FOREIGN KEY (pet_id) REFERENCES pet_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI情绪识别结果表';

-- ========================================
-- 5. 宠友圈社区模块 (com.itsworld.community) - 待完整设计
-- ========================================

-- 社区帖子表
CREATE TABLE IF NOT EXISTS community_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
  user_id BIGINT NOT NULL COMMENT '发布者ID',
  pet_id BIGINT COMMENT '关联宠物ID',
  title VARCHAR(200) COMMENT '帖子标题',
  content TEXT COMMENT '帖子内容',
  images_json JSON COMMENT '帖子图片URL数组',
  category VARCHAR(50) COMMENT '分类 (story,tips,question,help...)',

  like_count INT DEFAULT 0,
  comment_count INT DEFAULT 0,
  view_count INT DEFAULT 0,

  status TINYINT DEFAULT 1 COMMENT '状态 (1-已发布 0-草稿)',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,

  KEY idx_user_id (user_id),
  KEY idx_pet_id (pet_id),
  KEY idx_category (category),
  KEY idx_created_at (created_at),
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (pet_id) REFERENCES pet_profile(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子表';

-- 社区评论表
CREATE TABLE IF NOT EXISTS community_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  post_id BIGINT NOT NULL COMMENT '帖子ID',
  user_id BIGINT NOT NULL COMMENT '评论者ID',
  parent_comment_id BIGINT COMMENT '父评论ID（回复），NULL表示顶级评论',
  content VARCHAR(500) NOT NULL COMMENT '评论内容',

  like_count INT DEFAULT 0,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,

  KEY idx_post_id (post_id),
  KEY idx_user_id (user_id),
  KEY idx_parent_comment_id (parent_comment_id),
  FOREIGN KEY (post_id) REFERENCES community_post(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (parent_comment_id) REFERENCES community_comment(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区评论表';

-- 社区帖子点赞表
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

-- 用户关注表
CREATE TABLE IF NOT EXISTS user_follow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注关系ID',
  follower_id BIGINT NOT NULL COMMENT '关注者ID',
  following_id BIGINT NOT NULL COMMENT '被关注者ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_follower_following (follower_id, following_id),
  KEY idx_following_id (following_id),
  FOREIGN KEY (follower_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  FOREIGN KEY (following_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- ========================================
-- 6. 系统配置与日志表
-- ========================================

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
  id INT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
  config_value TEXT COMMENT '配置值',
  config_type VARCHAR(50) COMMENT '配置类型 (string,number,json...)',
  description VARCHAR(200),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  KEY idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  user_id BIGINT COMMENT '操作用户ID',
  operation_type VARCHAR(50) COMMENT '操作类型 (create,update,delete...)',
  module_name VARCHAR(50) COMMENT '模块名',
  resource_id BIGINT COMMENT '操作的资源ID',
  request_url VARCHAR(500) COMMENT '请求URL',
  request_method VARCHAR(10) COMMENT '请求方法(GET,POST...)',
  request_params TEXT COMMENT '请求参数',
  response_code INT COMMENT '响应码',
  ip_address VARCHAR(50) COMMENT '操作IP',
  user_agent VARCHAR(500) COMMENT '浏览器信息',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  KEY idx_user_id (user_id),
  KEY idx_operation_type (operation_type),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ========================================
-- 7. 初始化数据
-- ========================================

-- 插入系统配置示例
INSERT INTO sys_config (config_key, config_value, config_type, description) VALUES
('app.name', 'ItsWorld', 'string', '应用名称'),
('app.version', '1.0.0', 'string', '应用版本'),
('file.upload.max_size', '52428800', 'number', '文件上传最大大小（字节，默认50MB）'),
('diary.max_photos_per_day', '10', 'number', '每天最多上传日记照片数');

-- ========================================
-- 数据库初始化完成
-- ========================================
-- 表总数: 21 (含系统表)
-- 状态: 就绪
-- 备注:
--   1. 所有时间字段均为 TIMESTAMP，自动记录创建/更新时间
--   2. 使用 utf8mb4 字符集支持 emoji 表情
--   3. 关键字段有合理的索引优化查询
--   4. 软删除字段 (deleted_at) 用于保留历史数据
--   5. 图片URL存储于 MinIO，数据库只存 URL 路径
-- ========================================

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

-- 宠物身高 + 日记分类（增量迁移）
-- 已有数据卷不会重新执行 01-init.sql，需要手动执行本脚本。
-- 使用 information_schema 判断列是否存在，可重复执行。

SET @height_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'pet_profile' AND column_name = 'height_cm'
);
SET @sql := IF(
  @height_exists = 0,
  'ALTER TABLE pet_profile ADD COLUMN height_cm DECIMAL(5,2) COMMENT ''身高(cm)'' AFTER weight_kg',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @category_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'pet_diary' AND column_name = 'category'
);
SET @sql := IF(
  @category_exists = 0,
  'ALTER TABLE pet_diary ADD COLUMN category VARCHAR(20) COMMENT ''日记分类 (daily-日常, food-饮食, health-健康)'' AFTER diary_date',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- 04 用户角色字段（会员运营最小权限）
-- ========================================
-- 说明：本脚本只对空数据卷自动执行一次；已有数据卷需手动执行一次。
-- 角色：USER（普通用户）/ ADMIN（运营管理员）
-- ========================================

USE its_world;

ALTER TABLE sys_user
  ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN' AFTER status,
  ADD KEY idx_role (role);

-- 本地既有开发账号提升为管理员；不存在时不报错、无影响
UPDATE sys_user SET role = 'ADMIN' WHERE username = 'admin';

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

-- Execute once AFTER 01~12. Back up before deployment. Legacy points remain archived,
-- never automatically added to growth (no conversion rule has been approved).
USE its_world;
ALTER TABLE member_account ADD COLUMN member_expires_at DATETIME NULL COMMENT '付费会员到期时间';
ALTER TABLE member_gift ADD COLUMN growth_reward INT NOT NULL DEFAULT 0;
ALTER TABLE member_gift_record ADD COLUMN growth_amount INT NOT NULL DEFAULT 0;
-- Old gift points columns stay archived; new growth columns start at zero.
CREATE TABLE member_subscription_plan (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(100) NOT NULL,
 level_id BIGINT NOT NULL,
 price BIGINT NOT NULL,
 duration_days INT NOT NULL,
 status INT NOT NULL DEFAULT 1,
 CONSTRAINT fk_subscription_level FOREIGN KEY(level_id) REFERENCES member_level(id),
 CHECK (price > 0 AND duration_days > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE member_subscription_grant (
 order_id BIGINT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 level_id BIGINT NOT NULL,
 starts_at DATETIME NOT NULL,
 expires_at DATETIME NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_grant_user(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE member_reward_product (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(100) NOT NULL,
 growth_cost INT NOT NULL,
 stock INT NOT NULL,
 status INT NOT NULL DEFAULT 1,
 CHECK(growth_cost > 0 AND stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE member_redemption (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 redemption_no VARCHAR(64) NOT NULL UNIQUE,
 user_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL,
 name_snapshot VARCHAR(100) NOT NULL,
 growth_cost INT NOT NULL,
 request_key VARCHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 fulfilled_by BIGINT NULL,
 fulfilled_at DATETIME NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_redemption_request(user_id,request_key),
 KEY idx_redemption_user(user_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- No production-priced plans or redemption products are seeded.
INSERT INTO member_task(task_code,task_name,description,period_type,target_value,points_reward,growth_reward,status,sort_order)
VALUES ('PHOTO_UPLOAD','上传宠物照片','成功上传宠物或日记图片','DAILY',1,0,50,1,20),
 ('GROOMING_COMPLETE','完成一次美容服务','门店确认服务完成','DAILY',1,0,300,1,30)
ON DUPLICATE KEY UPDATE task_name=VALUES(task_name);
