-- ============================================================
-- 升级脚本：顾客登录改为手机号登录，user.phone 增加唯一索引
-- Database: order
-- MySQL 8.0+
-- 日期：2026-10-06
--
-- 说明：
--   * user.phone 作为顾客登录标识，按手机号建档/登录，需唯一
--   * 先清理重复手机号（仅保留 id 最小的一条），再加唯一索引，保证幂等可重放
--
-- 执行方式：在已存在旧库的情况下执行本脚本即可（幂等）。
-- ============================================================

USE `order`;

-- 1. 清理重复手机号：同一手机号仅保留 id 最小的一条，其余置空
--    （仅当 uk_phone 索引尚不存在时执行，避免重复清理影响已规范化数据）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_phone'
);

SET @cleanup := IF(@idx_exists = 0,
  'UPDATE `user` SET `phone` = NULL WHERE `id` NOT IN (SELECT keep_id FROM (SELECT MIN(id) AS keep_id FROM `user` WHERE `phone` IS NOT NULL GROUP BY `phone`) t)',
  'SELECT ''uk_phone already exists, skip cleanup'''
);
PREPARE stmt FROM @cleanup;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 若索引不存在则新增唯一索引 uk_phone
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_phone` (`phone`)',
  'SELECT ''user.uk_phone already exists, skip'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;