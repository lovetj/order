-- ============================================================
-- 升级脚本：餐桌表 dining_table 新增楼号、类型(大厅/包房)、别名等字段
-- Database: order
-- MySQL 8.0+
-- 日期：2026-10-09
--
-- 背景与需求：
--   * 店家-我的-桌位管理功能重构，桌位设置需要包含：
--     楼号(building_no)、类型(type: 大厅/包房)、别名(alias)、桌号(table_no)、人数(capacity)。
--   * 保留现有字段及业务逻辑，不影响点餐、扫码、开台、订单查询等现有功能。
--
-- 执行方式：对已存在旧库执行本脚本即可（幂等，可重复执行）。
-- ============================================================

USE `order`;

-- 1. 新增 building_no 楼号/楼层字段
SET @col_building_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_table' AND COLUMN_NAME = 'building_no'
);
SET @add_col_building := IF(@col_building_exists = 0,
  'ALTER TABLE `dining_table` ADD COLUMN `building_no` varchar(50) DEFAULT ''1楼'' COMMENT ''楼号/楼层，如 1楼、2楼'' AFTER `shop_id`',
  'SELECT ''dining_table.building_no already exists, skip'''
);
PREPARE stmt FROM @add_col_building;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 新增 type 类型字段 (大厅/包房)
SET @col_type_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_table' AND COLUMN_NAME = 'type'
);
SET @add_col_type := IF(@col_type_exists = 0,
  'ALTER TABLE `dining_table` ADD COLUMN `type` varchar(20) NOT NULL DEFAULT ''大厅'' COMMENT ''类型：大厅/包房'' AFTER `building_no`',
  'SELECT ''dining_table.type already exists, skip'''
);
PREPARE stmt FROM @add_col_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 新增 alias 别名字段
SET @col_alias_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_table' AND COLUMN_NAME = 'alias'
);
SET @add_col_alias := IF(@col_alias_exists = 0,
  'ALTER TABLE `dining_table` ADD COLUMN `alias` varchar(50) DEFAULT NULL COMMENT ''桌位别名，如 牡丹阁、VIP1、靠窗位'' AFTER `type`',
  'SELECT ''dining_table.alias already exists, skip'''
);
PREPARE stmt FROM @add_col_alias;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 补充 (shop_id, building_no) 索引（提升按楼号查询性能）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dining_table' AND INDEX_NAME = 'idx_shop_building'
);
SET @add_idx := IF(@idx_exists = 0,
  'ALTER TABLE `dining_table` ADD KEY `idx_shop_building` (`shop_id`, `building_no`)',
  'SELECT ''dining_table.idx_shop_building already exists, skip'''
);
PREPARE stmt FROM @add_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 初始化历史存量数据默认值（防止旧数据为空）
UPDATE `dining_table` SET `building_no` = '1楼' WHERE `building_no` IS NULL OR `building_no` = '';
UPDATE `dining_table` SET `type` = '大厅' WHERE `type` IS NULL OR `type` = '';
