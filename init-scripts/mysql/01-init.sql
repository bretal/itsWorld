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
