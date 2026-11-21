-- 支持同一只宠物一天记录多件事。执行前备份数据库。
-- 先建立普通索引，使宠物外键不再依赖旧唯一索引。
SET @add_index = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE pet_diary ADD INDEX idx_pet_date (pet_id, diary_date, id)', 'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'pet_diary' AND index_name = 'idx_pet_date'
);
PREPARE stmt FROM @add_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @drop_index = (
    SELECT IF(COUNT(*) > 0, 'ALTER TABLE pet_diary DROP INDEX uk_pet_date', 'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'pet_diary' AND index_name = 'uk_pet_date'
);
PREPARE stmt FROM @drop_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @add_column = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE pet_diary ADD COLUMN occurred_at DATETIME NULL COMMENT "事件发生时间"', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pet_diary' AND column_name = 'occurred_at'
);
PREPARE stmt FROM @add_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @add_index = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE pet_diary ADD INDEX idx_pet_date (pet_id, diary_date, id)', 'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'pet_diary' AND index_name = 'idx_pet_date'
);
PREPARE stmt FROM @add_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
