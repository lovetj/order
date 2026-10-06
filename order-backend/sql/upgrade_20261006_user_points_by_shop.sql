-- ============================================================
-- 升级脚本：积分由「全局 user 维度」改为「按商家(shop)隔离」
-- Database: order
-- MySQL 8.0+
-- 日期：2026-10-06
--
-- 背景：
--   * 旧设计积分余额存在 user.points（全局一维，无 shop_id），导致多个商家的
--     积分商品共用一个全局积分本。
--   * 用户会在不同商家就餐，积分由各商家各自推出，需按 (user_id, shop_id) 隔离。
--
-- 变更：
--   * 新增用户积分账本表 user_points，以 (user_id, shop_id) 为复合主键，
--     为每位顾客在每家店铺维护独立积分余额。
--   * user.points 旧字段保留（不再作为多店铺积分依据），仅做无 shopId 时的回退兼容。
--   * 历史 user.points 无法确认归属商家，故不迁移，重建按店账本。
--
-- 执行方式：对已存在旧库执行本脚本即可（幂等，可重复执行）。
-- ============================================================

USE `order`;

CREATE TABLE IF NOT EXISTS `user_points` (
  `user_id` varchar(128) NOT NULL COMMENT '用户ID',
  `shop_id` varchar(128) NOT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `points` int NOT NULL DEFAULT '0' COMMENT '该店铺积分余额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`, `shop_id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户积分账本表（按商家隔离）';

-- 幂等保障：若索引不存在则补充（防止历史版本导致的索引缺失）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_points' AND INDEX_NAME = 'idx_shop_id'
);
SET @add_idx := IF(@idx_exists = 0,
  'ALTER TABLE `user_points` ADD KEY `idx_shop_id` (`shop_id`)',
  'SELECT ''user_points.idx_shop_id already exists, skip'''
);
PREPARE stmt FROM @add_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;