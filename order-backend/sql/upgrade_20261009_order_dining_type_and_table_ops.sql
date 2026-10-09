-- ============================================================
-- 升级脚本：订单表支持堂食/外带区分，餐桌表支持使用状态管理（清台/拼桌/换桌）
-- Database: order
-- MySQL 8.0+
-- 日期：2026-10-09
--
-- 背景与需求：
--   1. 区分用户就餐是堂食还是外带：
--      - 订单表 order 新增 dining_type（1堂食 2外带）
--      - 外带不占用桌位，table_no 放宽允许外带标识或空
--   2. 餐桌管理支持清台、拼桌、换桌：
--      - dining_table 新增 use_status（0空闲 1使用中 2拼桌中 3待清台）
--      - dining_table 新增 current_order_count（当前进行中订单数）
--
-- 执行方式：对已存在旧库执行本脚本即可（幂等，可重复执行）。
-- ============================================================

USE `order`;

-- 1. order 表新增 dining_type 字段（1堂食 2外带）
SET @col_order_dining_type_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'dining_type'
);
SET @add_col_order_dining_type := IF(@col_order_dining_type_exists = 0,
  'ALTER TABLE `order` ADD COLUMN `dining_type` tinyint NOT NULL DEFAULT 1 COMMENT \'就餐方式 1堂食 2外带\' AFTER `table_no`',
  'SELECT \'order.dining_type already exists, skip\''
);
PREPARE stmt FROM @add_col_order_dining_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 放宽 order.table_no 约束（外带时允许非物理桌号，如 TAKEOUT 或空）
ALTER TABLE `order` MODIFY COLUMN `table_no` varchar(50) DEFAULT '' COMMENT '桌号(前端 table，外带时为TAKEOUT或空)';

-- 3. order 表为 dining_type 增加索引
SET @idx_order_dining_type_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_dining_type'
);
SET @add_idx_order_dining_type := IF(@idx_order_dining_type_exists = 0,
  'ALTER TABLE `order` ADD KEY `idx_dining_type` (`dining_type`)',
  'SELECT \'order.idx_dining_type already exists, skip\''
);
PREPARE stmt FROM @add_idx_order_dining_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. dining_table 表新增 use_status 使用状态字段（0空闲 1使用中 2拼桌中 3待清台）
SET @col_table_use_status_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_table' AND COLUMN_NAME = 'use_status'
);
SET @add_col_table_use_status := IF(@col_table_use_status_exists = 0,
  'ALTER TABLE `dining_table` ADD COLUMN `use_status` tinyint NOT NULL DEFAULT 0 COMMENT \'使用状态 0空闲 1使用中 2拼桌中 3待清台\' AFTER `status`',
  'SELECT \'dining_table.use_status already exists, skip\''
);
PREPARE stmt FROM @add_col_table_use_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. dining_table 表新增 current_order_count 字段
SET @col_table_current_orders_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_table' AND COLUMN_NAME = 'current_order_count'
);
SET @add_col_table_current_orders := IF(@col_table_current_orders_exists = 0,
  'ALTER TABLE `dining_table` ADD COLUMN `current_order_count` int NOT NULL DEFAULT 0 COMMENT \'当前进行中订单数\' AFTER `use_status`',
  'SELECT \'dining_table.current_order_count already exists, skip\''
);
PREPARE stmt FROM @add_col_table_current_orders;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. dining_table 表增加 idx_use_status 索引
SET @idx_table_use_status_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_table' AND INDEX_NAME = 'idx_use_status'
);
SET @add_idx_table_use_status := IF(@idx_table_use_status_exists = 0,
  'ALTER TABLE `dining_table` ADD KEY `idx_use_status` (`use_status`)',
  'SELECT \'dining_table.idx_use_status already exists, skip\''
);
PREPARE stmt FROM @add_idx_table_use_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
