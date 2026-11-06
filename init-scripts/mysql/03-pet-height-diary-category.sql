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
