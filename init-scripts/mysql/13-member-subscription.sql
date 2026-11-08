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
