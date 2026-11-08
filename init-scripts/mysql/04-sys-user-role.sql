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
