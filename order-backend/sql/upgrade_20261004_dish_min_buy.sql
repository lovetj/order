-- ============================================================
-- 升级脚本：菜品表新增「起购份数」字段
-- Database: order
-- MySQL 8.0+
-- 日期：2026-10-04
--
-- 说明：
--   * dish.min_buy  起购份数，1 表示一份起购（默认 1）
--   * 前端逻辑：min_buy = 1 时点餐展示加号；> 1 时展示「N 份起购」
--   * 下单金额按份数计算，且数量不得低于起购份数
--
-- 执行方式：在已存在旧库的情况下执行本脚本即可（幂等）。
-- ============================================================

USE `order`;

-- 若字段不存在则新增（兼容 MySQL 8.0，通过 information_schema 判断）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dish' AND COLUMN_NAME = 'min_buy'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `dish` ADD COLUMN `min_buy` int NOT NULL DEFAULT 1 COMMENT ''起购份数，1 表示一份起购'' AFTER `price`',
  'SELECT ''dish.min_buy already exists, skip'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 历史数据兜底：起购份数最小为 1
UPDATE `dish` SET `min_buy` = 1 WHERE `min_buy` IS NULL OR `min_buy` < 1;
