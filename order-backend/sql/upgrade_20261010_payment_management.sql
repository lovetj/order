-- ============================================================
-- 升级脚本：支付管理模块（线下支付开关/收款码、线上微信/支付宝支付开关、订单支付模式与流水表）
-- Database: order
-- MySQL 8.0+
-- 日期：2026-10-10
--
-- 背景与需求：
--   1. 店家端支持「支付管理」：
--      - 线下支付开关（开启后允许维护微信收款码、支付宝收款码、其他收款码图片）
--      - 线上微信支付开关
--      - 线上支付宝支付开关
--   2. 顾客端下单：
--      - 依据店铺配置动态展示支付方式，未开启的支付方式置灰不可选
--      - 选择线下支付可直接提交订单，展示提示信息供顾客扫描店内二维码付款，店家确认到账后手动完成订单
--      - 选择线上支付调用微信/支付宝支付能力（参考 mall-backend 微信支付/支付宝集成与 Mock 机制）
--   3. 订单表增加支付方式兼容、支付状态及线下收款相关跟踪字段
--   4. 新增支付流水表 payment 支撑第三方线上支付流水对账
--
-- 执行方式：对已存在旧库执行本脚本即可（幂等设计，可安全重复执行）。
-- ============================================================

USE `order`;

-- ------------------------------------------------------------
-- 1. 创建店铺支付配置表 shop_payment_config
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `shop_payment_config` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) NOT NULL COMMENT '所属店铺ID（多店铺隔离，唯一）',
  `offline_pay_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '线下支付总开关 0关闭 1开启',
  `offline_wx_qrcode` varchar(500) DEFAULT NULL COMMENT '线下支付-微信收款码图片URL',
  `offline_alipay_qrcode` varchar(500) DEFAULT NULL COMMENT '线下支付-支付宝收款码图片URL',
  `offline_other_qrcode` varchar(500) DEFAULT NULL COMMENT '线下支付-其他收款码/聚合码图片URL',
  `online_wx_pay_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '线上微信支付开关 0关闭 1开启',
  `online_alipay_pay_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '线上支付宝支付开关 0关闭 1开启',
  `wx_mch_id` varchar(64) DEFAULT NULL COMMENT '微信支付商户号(可选商户独立配置)',
  `wx_app_id` varchar(64) DEFAULT NULL COMMENT '微信支付AppId(可选商户独立配置)',
  `alipay_app_id` varchar(64) DEFAULT NULL COMMENT '支付宝应用AppId(可选商户独立配置)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='店铺支付管理配置表';

-- 1.1 若存在历史字段 offline_pay_desc，则安全移除
SET @col_shop_pay_desc_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop_payment_config' AND COLUMN_NAME = 'offline_pay_desc'
);
SET @drop_col_shop_pay_desc := IF(@col_shop_pay_desc_exists > 0,
  'ALTER TABLE `shop_payment_config` DROP COLUMN `offline_pay_desc`',
  'SELECT \'shop_payment_config.offline_pay_desc does not exist, skip\''
);
PREPARE stmt_drop_desc FROM @drop_col_shop_pay_desc;
EXECUTE stmt_drop_desc;
DEALLOCATE PREPARE stmt_drop_desc;

-- ------------------------------------------------------------
-- 2. 订单表 `order` 字段更新与扩展
-- ------------------------------------------------------------

-- 2.1 修改 pay_type 字段注释，兼容线上与线下细分方式
ALTER TABLE `order` MODIFY COLUMN `pay_type` tinyint DEFAULT 99 COMMENT '支付方式 1微信线上 2支付宝线上 3线下微信 4线下支付宝 5线下其他 99未支付';

-- 2.2 增加 pay_mode 字段（1线上支付 2线下支付）
SET @col_order_pay_mode_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'pay_mode'
);
SET @add_col_order_pay_mode := IF(@col_order_pay_mode_exists = 0,
  'ALTER TABLE `order` ADD COLUMN `pay_mode` tinyint NOT NULL DEFAULT 1 COMMENT \'支付大类 1线上支付 2线下支付\' AFTER `pay_type`',
  'SELECT \'order.pay_mode already exists, skip\''
);
PREPARE stmt FROM @add_col_order_pay_mode;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.3 增加 pay_status 字段（0待支付 1支付中 2已支付 3支付失败/已关闭）
SET @col_order_pay_status_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'pay_status'
);
SET @add_col_order_pay_status := IF(@col_order_pay_status_exists = 0,
  'ALTER TABLE `order` ADD COLUMN `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT \'支付状态 0待支付 1支付中 2已支付 3支付失败/已关闭\' AFTER `pay_mode`',
  'SELECT \'order.pay_status already exists, skip\''
);
PREPARE stmt FROM @add_col_order_pay_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.4 增加 pay_qrcode 字段（线下支付二维码快照URL）
SET @col_order_pay_qrcode_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'pay_qrcode'
);
SET @add_col_order_pay_qrcode := IF(@col_order_pay_qrcode_exists = 0,
  'ALTER TABLE `order` ADD COLUMN `pay_qrcode` varchar(500) DEFAULT NULL COMMENT \'线下支付收款二维码快照URL\' AFTER `pay_status`',
  'SELECT \'order.pay_qrcode already exists, skip\''
);
PREPARE stmt FROM @add_col_order_pay_qrcode;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.5 增加 payment_no 字段（关联线上支付流水号）
SET @col_order_payment_no_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'payment_no'
);
SET @add_col_order_payment_no := IF(@col_order_payment_no_exists = 0,
  'ALTER TABLE `order` ADD COLUMN `payment_no` varchar(64) DEFAULT NULL COMMENT \'关联支付流水号\' AFTER `pay_qrcode`',
  'SELECT \'order.payment_no already exists, skip\''
);
PREPARE stmt FROM @add_col_order_payment_no;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.6 增加 offline_pay_time 字段（线下支付店员确认到账时间）
SET @col_order_offline_pay_time_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'offline_pay_time'
);
SET @add_col_order_offline_pay_time := IF(@col_order_offline_pay_time_exists = 0,
  'ALTER TABLE `order` ADD COLUMN `offline_pay_time` datetime DEFAULT NULL COMMENT \'线下支付店员确认到账时间\' AFTER `pay_time`',
  'SELECT \'order.offline_pay_time already exists, skip\''
);
PREPARE stmt FROM @add_col_order_offline_pay_time;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.7 增加 offline_pay_operator 字段（线下支付确认操作人/店员ID）
SET @col_order_offline_operator_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'offline_pay_operator'
);
SET @add_col_order_offline_operator := IF(@col_order_offline_operator_exists = 0,
  'ALTER TABLE `order` ADD COLUMN `offline_pay_operator` varchar(64) DEFAULT NULL COMMENT \'线下支付确认操作人/店员ID\' AFTER `offline_pay_time`',
  'SELECT \'order.offline_pay_operator already exists, skip\''
);
PREPARE stmt FROM @add_col_order_offline_operator;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.8 增加索引
SET @idx_order_pay_status_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_pay_status'
);
SET @add_idx_order_pay_status := IF(@idx_order_pay_status_exists = 0,
  'ALTER TABLE `order` ADD KEY `idx_pay_status` (`pay_status`)',
  'SELECT \'order.idx_pay_status already exists, skip\''
);
PREPARE stmt FROM @add_idx_order_pay_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_order_payment_no_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND INDEX_NAME = 'idx_payment_no'
);
SET @add_idx_order_payment_no := IF(@idx_order_payment_no_exists = 0,
  'ALTER TABLE `order` ADD KEY `idx_payment_no` (`payment_no`)',
  'SELECT \'order.idx_payment_no already exists, skip\''
);
PREPARE stmt FROM @add_idx_order_payment_no;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3. 创建支付流水表 payment（拉齐 mall-backend 并支持多店铺隔离）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `payment` (
  `id` varchar(128) NOT NULL COMMENT '主键',
  `payment_no` varchar(32) NOT NULL COMMENT '本系统支付流水号(P开头, 唯一)',
  `shop_id` varchar(128) DEFAULT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `order_id` varchar(128) NOT NULL COMMENT '关联订单ID',
  `order_no` varchar(50) NOT NULL COMMENT '关联订单号(冗余, 方便查询)',
  `user_id` varchar(128) NOT NULL COMMENT '用户ID',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '支付金额(单位元)',
  `pay_type` tinyint NOT NULL COMMENT '支付方式 1微信线上 2支付宝线上 3线下微信 4线下支付宝 5线下其他 99Mock',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态 0待支付 1已支付成功 2支付失败 3已关闭 4已退款',
  `transaction_id` varchar(64) DEFAULT NULL COMMENT '微信/支付宝第三方交易号',
  `prepay_id` varchar(64) DEFAULT NULL COMMENT '微信预支付ID(JSAPI/APP场景)',
  `qr_code_url` varchar(512) DEFAULT NULL COMMENT '二维码链接(Native/扫码场景)',
  `mweb_url` varchar(512) DEFAULT NULL COMMENT 'H5支付跳转链接(MWEB场景)',
  `request_param` text COMMENT '下单请求参数(JSON快照, 便于排查)',
  `notify_data` text COMMENT '第三方回调原始报文(JSON)',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '下单客户端IP',
  `pay_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `expire_time` datetime DEFAULT NULL COMMENT '订单过期时间(通常下单后30分钟)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付流水表';

-- ------------------------------------------------------------
-- 4. 插入演示店铺初始配置数据（如已存在则忽略）
-- ------------------------------------------------------------
INSERT INTO `shop_payment_config` (`id`, `shop_id`, `offline_pay_enabled`, `offline_wx_qrcode`, `offline_alipay_qrcode`, `offline_other_qrcode`, `offline_pay_desc`, `online_wx_pay_enabled`, `online_alipay_pay_enabled`)
VALUES 
('spc001', '1', 1, NULL, NULL, NULL, '请向店员出示付款码或扫描店内二维码支付', 1, 1),
('spc002', '2', 1, NULL, NULL, NULL, '分店支持线下扫码付款及线上支付', 1, 1)
ON DUPLICATE KEY UPDATE `update_time` = NOW();
