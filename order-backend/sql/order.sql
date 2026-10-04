-- ============================================================
-- 扫码点餐小程序 数据库脚本
-- Database: order
-- MySQL 8.0+
-- 说明：字段与前端 order-frontend 的数据结构严格对齐
--   * dish.status        1上架/0下架   -> 前端 status: boolean
--   * dish.description                  -> 前端 desc
--   * order_item.quantity               -> 前端 count
--   * order.status  0待接单/1制作中/2已完成/3已取消
--                                      -> 前端 pending/cooking/done/canceled
--   * order.table_no                    -> 前端 table
-- ============================================================

CREATE DATABASE IF NOT EXISTS `order` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `order`;

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 门店表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `shop`;
CREATE TABLE `shop` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '门店名称',
  `real_name` varchar(50) DEFAULT NULL COMMENT '店家真实姓名',
  `logo` varchar(255) DEFAULT NULL COMMENT '门店Logo',
  `images` varchar(2000) DEFAULT NULL COMMENT '店铺图片(多张,逗号分隔)',
  `slogan` varchar(255) DEFAULT NULL COMMENT '宣传语',
  `notice` varchar(500) DEFAULT NULL COMMENT '店内公告（多条用 | 分隔）',
  `address` varchar(255) DEFAULT NULL COMMENT '门店地址',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '店铺定位纬度（微信地图选点）',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '店铺定位经度（微信地图选点）',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `score` decimal(3,1) DEFAULT '5.0' COMMENT '评分',
  `month_sales` int DEFAULT '0' COMMENT '月销量',
  `business_status` tinyint DEFAULT '1' COMMENT '营业状态 0休息中 1营业中',
  `status` tinyint DEFAULT '1' COMMENT '状态 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='门店表';

INSERT INTO `shop` (`id`, `name`, `real_name`, `logo`, `slogan`, `notice`, `address`, `latitude`, `longitude`, `phone`, `score`, `month_sales`, `business_status`, `status`) VALUES
('1', '巷子口小馆', '张老板', NULL, '现点现做 · 用心出餐', '本店支持扫码点餐，出餐后请耐心等待|满 60 元赠送饮品一杯|营业时间 10:00 - 22:00', '某市某区巷子口 1 号', 39.908823, 116.397470, '13800138000', 4.9, 2680, 1, 1),
('2', '第二家分店', '李老板', NULL, '分店开业 · 欢迎光临',    '本店支持扫码点餐，出餐后请耐心等待|营业时间 09:00 - 21:00', '某市某区中心路 88 号', 39.918823, 116.407470, '13800138002', 4.8, 1200, 1, 1);

-- ------------------------------------------------------------
-- 店家（商家）账号表
-- 默认密码：admin123 (BCrypt)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录用户名',
  `password` varchar(100) NOT NULL COMMENT '密码(BCrypt)',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `shop_id` varchar(128) DEFAULT NULL COMMENT '关联门店ID',
  `status` tinyint DEFAULT '1' COMMENT '状态 0禁用 1正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='店家账号表';

-- 说明：真实姓名已迁移到 shop.real_name，手机号统一使用 shop.phone，本表不再冗余存储
INSERT INTO `admin` (`id`, `username`, `password`, `avatar`, `shop_id`, `status`) VALUES
('1', 'admin',  '$2a$10$f3o1ECZ6a.VZhezkmhl46uBpGmnPNmLvsRDIdjkJ/a/oNodOZJu0S', NULL, '1', 1),
-- 第二个店家账号（密码：admin123），用于验证多店铺隔离
('2', 'admin2', '$2a$10$f3o1ECZ6a.VZhezkmhl46uBpGmnPNmLvsRDIdjkJ/a/oNodOZJu0S', NULL, '2', 1);

-- ------------------------------------------------------------
-- 顾客用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码(微信免密用户可为空)',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `openid` varchar(64) DEFAULT NULL COMMENT '微信小程序openid',
  `unionid` varchar(64) DEFAULT NULL COMMENT '微信开放平台unionid',
  `session_key` varchar(128) DEFAULT NULL COMMENT '微信会话密钥session_key(仅服务端保存)',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `member_level` varchar(20) DEFAULT '普通会员' COMMENT '会员等级',
  `points` int DEFAULT '0' COMMENT '积分余额',
  `total_consume` decimal(12,2) DEFAULT '0.00' COMMENT '累计消费金额',
  `order_count` int DEFAULT '0' COMMENT '累计订单数',
  `status` tinyint DEFAULT '1' COMMENT '状态 0禁用 1正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='顾客用户表';

INSERT INTO `user` (`id`, `username`, `password`, `phone`, `openid`, `avatar`, `nickname`, `member_level`, `points`, `total_consume`, `order_count`, `status`) VALUES
('u001', 'user', '$2a$10$rjet.LriLzGqBZ9StUj0QeBKAnojolx.fgAqVWlhC7KgvRiQSb4z.', '13800138001', NULL, '🙋', '测试用户', '黄金会员', 268, 2280.00, 86, 1);

-- ------------------------------------------------------------
-- 菜品分类表
-- 前端 data.js CATEGORIES: hot/staple/soup/drink/snack
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) NOT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `code` varchar(50) NOT NULL COMMENT '分类编码',
  `icon` varchar(255) DEFAULT NULL COMMENT '分类图标',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_code` (`shop_id`, `code`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品分类表';

INSERT INTO `category` (`id`, `shop_id`, `name`, `code`, `icon`, `sort`, `status`) VALUES
('hot',    '1', '热销推荐', 'hot',    NULL, 1, 1),
('staple', '1', '主食',     'staple', NULL, 2, 1),
('soup',   '1', '汤羹',     'soup',   NULL, 3, 1),
('drink',  '1', '饮品',     'drink',  NULL, 4, 1),
('snack',  '1', '小吃',     'snack',  NULL, 5, 1);

-- ------------------------------------------------------------
-- 菜品表
-- 前端 data.js GOODS 字段映射:
--   description -> desc, status(1/0) -> status(true/false)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) NOT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `category_id` varchar(128) NOT NULL COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '菜品名称',
  `description` varchar(255) DEFAULT NULL COMMENT '菜品描述(前端 desc)',
  `image` varchar(500) DEFAULT NULL COMMENT '菜品图片/emoji',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '售价',
  `min_buy` int NOT NULL DEFAULT '1' COMMENT '起购份数，1 表示一份起购',
  `stock` int DEFAULT '999' COMMENT '库存',
  `sales` int DEFAULT '0' COMMENT '销量',
  `is_hot` tinyint DEFAULT '0' COMMENT '是否热销推荐 0否 1是',
  `status` tinyint DEFAULT '1' COMMENT '状态 0下架 1上架(前端 status:boolean)',
  `sort` int DEFAULT '0' COMMENT '排序',
  `is_del` tinyint DEFAULT '0' COMMENT '是否删除 0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_del` (`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品表';

INSERT INTO `dish` (`id`, `shop_id`, `category_id`, `name`, `description`, `image`, `price`, `min_buy`, `stock`, `sales`, `is_hot`, `status`, `sort`) VALUES
('1',  '1', 'hot',    '招牌红烧肉', '肥而不腻，入口即化',   '🥘', 38.00, 1, 100, 328, 1, 1, 1),
('2',  '1', 'hot',    '宫保鸡丁',   '花生香脆，微辣下饭',   '🍗', 28.00, 1, 100, 256, 1, 1, 2),
('3',  '1', 'hot',    '酸菜鱼',     '酸辣开胃，鱼肉嫩滑',   '🐟', 58.00, 1, 100, 189, 1, 1, 3),
('4',  '1', 'staple', '扬州炒饭',   '粒粒分明，配料丰富',   '🍚', 18.00, 1, 100, 421, 1, 1, 1),
('5',  '1', 'staple', '手工水饺',   '12只 / 份，皮薄馅大',  '🥟', 22.00, 2, 100, 167, 0, 1, 2),
('6',  '1', 'staple', '牛肉拉面',   '现拉现煮，汤头浓郁',   '🍜', 26.00, 1, 100, 233, 0, 1, 3),
('7',  '1', 'soup',   '西红柿蛋汤', '家常口味，酸甜可口',   '🍲', 12.00, 1, 100, 302, 0, 1, 1),
('8',  '1', 'soup',   '菌菇鸡汤',   '文火慢炖两小时',       '🥣', 32.00, 1, 100, 98,  0, 1, 2),
('9',  '1', 'drink',  '鲜榨橙汁',   '每日现榨，无添加',     '🧃', 16.00, 1, 100, 388, 0, 1, 1),
('10', '1', 'drink',  '柠檬冰红茶', '冰爽解腻',             '🥤', 10.00, 2, 100, 512, 1, 1, 2),
('11', '1', 'snack',  '黄金薯条',   '外酥里嫩',             '🍟', 14.00, 1, 100, 276, 0, 1, 1),
('12', '1', 'snack',  '香辣鸡翅',   '4只 / 份',             '🍖', 24.00, 2, 100, 198, 0, 1, 2);

-- ------------------------------------------------------------
-- 订单表
-- 前端订单卡片字段: { id(订单号), table, status, statusText, createTime, amount, items }
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单号，如 D20261003001',
  `user_id` varchar(128) NOT NULL COMMENT '下单用户ID',
  `shop_id` varchar(128) DEFAULT NULL COMMENT '门店ID',
  `table_no` varchar(20) NOT NULL COMMENT '桌号(前端 table)',
  `people_count` int DEFAULT NULL COMMENT '就餐人数',
  `product_total` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '菜品小计',
  `discount_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '优惠金额',
  `pay_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '实付金额(前端 amount)',
  `pay_type` tinyint DEFAULT '99' COMMENT '支付方式 1微信 2支付宝 99未支付',
  `status` tinyint DEFAULT '0' COMMENT '状态 0待接单 1制作中 2已完成 3已取消',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `accept_time` datetime DEFAULT NULL COMMENT '接单时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消/拒单原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间(前端 createTime)',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_table_no` (`table_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- ------------------------------------------------------------
-- 订单明细表
-- 前端 order.items 字段: { name, count, price }
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) DEFAULT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `order_id` varchar(128) NOT NULL COMMENT '订单ID',
  `dish_id` varchar(128) NOT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) NOT NULL COMMENT '菜品名称(前端 name，下单快照)',
  `dish_image` varchar(500) DEFAULT NULL COMMENT '菜品图片(下单快照)',
  `price` decimal(10,2) NOT NULL COMMENT '单价(下单快照，含规格加价)',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量(前端 count)',
  `amount` decimal(10,2) NOT NULL COMMENT '小计(=price*quantity)',
  `spec_text` varchar(255) DEFAULT NULL COMMENT '规格描述(下单快照，如 微辣,加蛋)',
  `spec_price` decimal(10,2) DEFAULT '0.00' COMMENT '规格加价合计',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';

-- ------------------------------------------------------------
-- 初始化订单演示数据（与前端 mock 数据一致）
-- ------------------------------------------------------------
INSERT INTO `order` (`id`, `order_no`, `user_id`, `shop_id`, `table_no`, `people_count`, `product_total`, `discount_amount`, `pay_amount`, `pay_type`, `status`, `remark`, `create_time`) VALUES
('o001', 'D20261003001', 'u001', '1', 'A01', 2, 92.00, 0.00, 92.00, 1, 0, '不要葱',      '2026-10-03 12:18:00'),
('o002', 'D20261003002', 'u001', '1', 'A03', 1, 68.00, 0.00, 68.00, 1, 1, NULL,          '2026-10-03 12:25:00'),
('o003', 'D20261003003', 'u001', '1', 'B02', 3, 46.00, 0.00, 46.00, 1, 2, NULL,          '2026-10-03 11:40:00'),
('o004', 'D20261003004', 'u001', '1', 'A05', 1, 22.00, 0.00, 22.00, 1, 3, '临时有事',    '2026-10-03 11:05:00');

INSERT INTO `order_item` (`id`, `order_id`, `dish_id`, `dish_name`, `dish_image`, `price`, `quantity`, `amount`) VALUES
('oi001', 'o001', '1',  '招牌红烧肉', '🥘', 38.00, 1, 38.00),
('oi002', 'o001', '4',  '扬州炒饭',   '🍚', 18.00, 2, 36.00),
('oi003', 'o001', '9',  '鲜榨橙汁',   '🧃', 16.00, 1, 16.00),
('oi004', 'o002', '3',  '酸菜鱼',     '🐟', 58.00, 1, 58.00),
('oi005', 'o002', '10', '柠檬冰红茶', '🥤', 10.00, 1, 10.00),
('oi006', 'o003', '2',  '宫保鸡丁',   '🍗', 28.00, 1, 28.00),
('oi007', 'o003', '11', '黄金薯条',   '🍟', 14.00, 1, 14.00),
('oi008', 'o004', '5',  '手工水饺',   '🥟', 22.00, 1, 22.00);

-- 种子明细统一归属门店 1（多店铺隔离：shop_id）
UPDATE `order_item` SET `shop_id` = '1' WHERE `shop_id` IS NULL;

-- ------------------------------------------------------------
-- 餐桌表（扫码点餐二维码管理）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `dining_table`;
CREATE TABLE `dining_table` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) NOT NULL COMMENT '门店ID',
  `table_no` varchar(20) NOT NULL COMMENT '桌号，如 A01',
  `capacity` int DEFAULT '4' COMMENT '容纳人数',
  `qr_code` varchar(500) DEFAULT NULL COMMENT '桌位二维码图片',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_table` (`shop_id`, `table_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='餐桌表';

INSERT INTO `dining_table` (`id`, `shop_id`, `table_no`, `capacity`, `status`) VALUES
('t001', '1', 'A01', 2, 1),
('t002', '1', 'A02', 4, 1),
('t003', '1', 'A03', 4, 1),
('t004', '1', 'A05', 6, 1),
('t005', '1', 'B02', 4, 1),
('t006', '1', 'B12', 8, 1);

-- ------------------------------------------------------------
-- 优惠券模板表
-- 类型 1满减 2折扣
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) NOT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `name` varchar(100) NOT NULL COMMENT '券名称',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '类型 1满减 2折扣',
  `threshold` decimal(10,2) DEFAULT '0.00' COMMENT '使用门槛(满多少可用)，0 表示无门槛',
  `amount` decimal(10,2) DEFAULT '0.00' COMMENT '优惠金额(满减券)',
  `discount` decimal(4,2) DEFAULT NULL COMMENT '折扣率(折扣券，如 0.88 表示 8.8 折)',
  `total_count` int DEFAULT '-1' COMMENT '发放总量，-1 表示不限量',
  `issued_count` int DEFAULT '0' COMMENT '已发放数量',
  `per_limit` int DEFAULT '1' COMMENT '每人限领数量',
  `valid_days` int DEFAULT '0' COMMENT '领取后有效天数，0 表示用固定起止时间',
  `start_time` datetime DEFAULT NULL COMMENT '固定有效期开始',
  `end_time` datetime DEFAULT NULL COMMENT '固定有效期结束',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1启用',
  `description` varchar(255) DEFAULT NULL COMMENT '使用说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='优惠券模板表';

INSERT INTO `coupon` (`id`, `shop_id`, `name`, `type`, `threshold`, `amount`, `discount`, `total_count`, `issued_count`, `per_limit`, `valid_days`, `status`, `description`) VALUES
('c001', '1', '新客立减 15 元', 1, 0.00,   15.00, NULL, -1,   0, 1, 30, 1, '扫码点餐专享，无门槛使用'),
('c002', '1', '满 100 减 20',   1, 100.00, 20.00, NULL, 1000, 0, 1, 30, 1, '堂食全场通用'),
('c003', '1', '招牌菜品 8 折',  2, 50.00,  0.00,  0.80, 500,  0, 1, 15, 1, '每日限量供应，满 50 元可用'),
('c004', '1', '满 60 减 10',    1, 60.00,  10.00, NULL, -1,   0, 2, 30, 1, '通用满减券，每人限领 2 张');

-- ------------------------------------------------------------
-- 用户优惠券表
-- 状态：0未使用 1已使用 2已过期
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) DEFAULT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `user_id` varchar(128) NOT NULL COMMENT '用户ID',
  `coupon_id` varchar(128) NOT NULL COMMENT '优惠券模板ID',
  `coupon_name` varchar(100) NOT NULL COMMENT '券名称(快照)',
  `type` tinyint NOT NULL COMMENT '类型 1满减 2折扣(快照)',
  `threshold` decimal(10,2) DEFAULT '0.00' COMMENT '门槛金额(快照)',
  `amount` decimal(10,2) DEFAULT '0.00' COMMENT '优惠金额(快照)',
  `discount` decimal(4,2) DEFAULT NULL COMMENT '折扣率(快照)',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0未使用 1已使用 2已过期',
  `start_time` datetime DEFAULT NULL COMMENT '生效时间',
  `end_time` datetime DEFAULT NULL COMMENT '过期时间',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `order_id` varchar(128) DEFAULT NULL COMMENT '关联订单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_status` (`status`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户优惠券表';

INSERT INTO `user_coupon` (`id`, `shop_id`, `user_id`, `coupon_id`, `coupon_name`, `type`, `threshold`, `amount`, `discount`, `status`, `start_time`, `end_time`) VALUES
('uc001', '1', 'u001', 'c001', '新客立减 15 元', 1, 0.00,   15.00, NULL, 0, '2026-10-01 00:00:00', '2026-12-31 23:59:59'),
('uc002', '1', 'u001', 'c002', '满 100 减 20',   1, 100.00, 20.00, NULL, 0, '2026-10-01 00:00:00', '2026-12-31 23:59:59'),
('uc003', '1', 'u001', 'c004', '满 60 减 10',    1, 60.00,  10.00, NULL, 0, '2026-10-01 00:00:00', '2026-12-31 23:59:59');

-- ------------------------------------------------------------
-- 菜品规格/口味选项表
-- 选择类型 1单选 2多选
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `dish_spec`;
CREATE TABLE `dish_spec` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) DEFAULT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `dish_id` varchar(128) NOT NULL COMMENT '菜品ID',
  `group_name` varchar(50) NOT NULL COMMENT '规格分组名，如 辣度/加料/温度',
  `name` varchar(50) NOT NULL COMMENT '选项名，如 微辣/加蛋',
  `extra_price` decimal(10,2) DEFAULT '0.00' COMMENT '加价',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认选中 0否 1是',
  `select_type` tinyint DEFAULT '1' COMMENT '选择类型 1单选 2多选',
  `required` tinyint DEFAULT '0' COMMENT '是否必选 0否 1是',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1启用',
  `is_del` tinyint DEFAULT '0' COMMENT '是否删除 0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品规格/口味选项表';

INSERT INTO `dish_spec` (`id`, `dish_id`, `group_name`, `name`, `extra_price`, `is_default`, `select_type`, `required`, `sort`, `status`) VALUES
-- ========== 1 招牌红烧肉：辣度(必选单选) + 加料(多选) ==========
('ds101', '1', '辣度', '不辣',   0.00, 1, 1, 1, 1, 1),
('ds102', '1', '辣度', '微辣',   0.00, 0, 1, 1, 2, 1),
('ds103', '1', '辣度', '中辣',   0.00, 0, 1, 1, 3, 1),
('ds104', '1', '辣度', '特辣',   0.00, 0, 1, 1, 4, 1),
('ds105', '1', '加料', '加卤蛋', 2.00, 0, 2, 0, 1, 1),
('ds106', '1', '加料', '加青菜', 3.00, 0, 2, 0, 2, 1),
('ds107', '1', '加料', '加米饭', 2.00, 0, 2, 0, 3, 1),
-- ========== 2 宫保鸡丁：辣度(必选单选) + 加料(多选) ==========
('ds201', '2', '辣度', '微辣',   0.00, 1, 1, 1, 1, 1),
('ds202', '2', '辣度', '中辣',   0.00, 0, 1, 1, 2, 1),
('ds203', '2', '辣度', '特辣',   0.00, 0, 1, 1, 3, 1),
('ds204', '2', '加料', '加花生', 2.00, 0, 2, 0, 1, 1),
('ds205', '2', '加料', '加米饭', 2.00, 0, 2, 0, 2, 1),
-- ========== 3 酸菜鱼：辣度(必选单选) + 加料(多选) ==========
('ds301', '3', '辣度', '微辣',   0.00, 0, 1, 1, 1, 1),
('ds302', '3', '辣度', '中辣',   0.00, 1, 1, 1, 2, 1),
('ds303', '3', '辣度', '特辣',   0.00, 0, 1, 1, 3, 1),
('ds304', '3', '加料', '加宽粉', 4.00, 0, 2, 0, 1, 1),
('ds305', '3', '加料', '加豆芽', 3.00, 0, 2, 0, 2, 1),
-- ========== 4 扬州炒饭：份量(必选单选) + 加料(多选) ==========
('ds401', '4', '份量', '标准份', 0.00, 1, 1, 1, 1, 1),
('ds402', '4', '份量', '大份',   5.00, 0, 1, 1, 2, 1),
('ds403', '4', '加料', '加火腿', 3.00, 0, 2, 0, 1, 1),
('ds404', '4', '加料', '加虾仁', 6.00, 0, 2, 0, 2, 1),
-- ========== 5 手工水饺：份量(必选单选) + 加料(多选) ==========
('ds501', '5', '份量', '12只',   0.00, 1, 1, 1, 1, 1),
('ds502', '5', '份量', '20只',   10.00, 0, 1, 1, 2, 1),
('ds503', '5', '加料', '加醋碟', 0.00, 0, 2, 0, 1, 1),
('ds504', '5', '加料', '加辣油', 0.00, 0, 2, 0, 2, 1),
-- ========== 6 牛肉拉面：面型(必选单选) + 辣度(必选单选) + 加料(多选) ==========
('ds601', '6', '面型', '细面',   0.00, 1, 1, 1, 1, 1),
('ds602', '6', '面型', '宽面',   0.00, 0, 1, 1, 2, 1),
('ds603', '6', '面型', '刀削面', 2.00, 0, 1, 1, 3, 1),
('ds604', '6', '辣度', '不辣',   0.00, 0, 1, 1, 1, 1),
('ds605', '6', '辣度', '微辣',   0.00, 1, 1, 1, 2, 1),
('ds606', '6', '辣度', '特辣',   0.00, 0, 1, 1, 3, 1),
('ds607', '6', '加料', '加牛肉', 8.00, 0, 2, 0, 1, 1),
('ds608', '6', '加料', '加卤蛋', 2.00, 0, 2, 0, 2, 1),
-- ========== 7 西红柿蛋汤：份量(必选单选) ==========
('ds701', '7', '份量', '例份',   0.00, 1, 1, 1, 1, 1),
('ds702', '7', '份量', '大份',   6.00, 0, 1, 1, 2, 1),
-- ========== 8 菌菇鸡汤：份量(必选单选) ==========
('ds801', '8', '份量', '例份',   0.00, 1, 1, 1, 1, 1),
('ds802', '8', '份量', '大份',   10.00, 0, 1, 1, 2, 1),
-- ========== 9 鲜榨橙汁：温度(必选单选) + 糖度(必选单选) ==========
('ds901', '9', '温度', '常温',   0.00, 0, 1, 1, 1, 1),
('ds902', '9', '温度', '加冰',   0.00, 1, 1, 1, 2, 1),
('ds903', '9', '糖度', '全糖',   0.00, 1, 1, 1, 1, 1),
('ds904', '9', '糖度', '半糖',   0.00, 0, 1, 1, 2, 1),
('ds905', '9', '糖度', '无糖',   0.00, 0, 1, 1, 3, 1),
-- ========== 10 柠檬冰红茶：温度(必选单选) + 糖度(必选单选) ==========
('ds1001', '10', '温度', '常温',   0.00, 0, 1, 1, 1, 1),
('ds1002', '10', '温度', '加冰',   0.00, 1, 1, 1, 2, 1),
('ds1003', '10', '糖度', '全糖',   0.00, 1, 1, 1, 1, 1),
('ds1004', '10', '糖度', '半糖',   0.00, 0, 1, 1, 2, 1),
('ds1005', '10', '糖度', '无糖',   0.00, 0, 1, 1, 3, 1),
-- ========== 11 黄金薯条：份量(必选单选) + 加料(多选) ==========
('ds1101', '11', '份量', '标准份', 0.00, 1, 1, 1, 1, 1),
('ds1102', '11', '份量', '大份',   6.00, 0, 1, 1, 2, 1),
('ds1103', '11', '加料', '加番茄酱', 1.00, 0, 2, 0, 1, 1),
('ds1104', '11', '加料', '加芝士酱', 3.00, 0, 2, 0, 2, 1),
-- ========== 12 香辣鸡翅：辣度(必选单选) + 份量(必选单选) ==========
('ds1201', '12', '辣度', '微辣',   0.00, 1, 1, 1, 1, 1),
('ds1202', '12', '辣度', '中辣',   0.00, 0, 1, 1, 2, 1),
('ds1203', '12', '辣度', '特辣',   0.00, 0, 1, 1, 3, 1),
('ds1204', '12', '份量', '4只',    0.00, 1, 1, 1, 1, 1),
('ds1205', '12', '份量', '8只',    20.00, 0, 1, 1, 2, 1);

-- 种子规格统一归属门店 1（多店铺隔离：shop_id）
UPDATE `dish_spec` SET `shop_id` = '1' WHERE `shop_id` IS NULL;

-- ------------------------------------------------------------
-- 积分商城商品表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `points_goods`;
CREATE TABLE `points_goods` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) NOT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `name` varchar(100) NOT NULL COMMENT '商品名称',
  `description` varchar(255) DEFAULT NULL COMMENT '商品描述',
  `image` varchar(500) DEFAULT NULL COMMENT '商品图片/emoji',
  `points` int NOT NULL DEFAULT '0' COMMENT '兑换所需积分',
  `stock` int DEFAULT '-1' COMMENT '库存，-1 表示不限量',
  `exchanged_count` int DEFAULT '0' COMMENT '已兑换数量',
  `per_limit` int DEFAULT '1' COMMENT '每人限兑数量',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0下架 1上架',
  `is_del` tinyint DEFAULT '0' COMMENT '是否删除 0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分商城商品表';

INSERT INTO `points_goods` (`id`, `shop_id`, `name`, `description`, `image`, `points`, `stock`, `exchanged_count`, `per_limit`, `sort`, `status`) VALUES
('pg001', '1', '招牌红烧肉（小份）', '凭核销码到店兑换', '🥘', 300, 50, 0, 1, 1, 1),
('pg002', '1', '柠檬冰红茶',        '凭核销码到店兑换', '🥤', 80,  -1, 0, 2, 2, 1),
('pg003', '1', '鲜榨橙汁',          '凭核销码到店兑换', '🧃', 120, -1, 0, 2, 3, 1),
('pg004', '1', '黄金薯条',          '凭核销码到店兑换', '🍟', 100, 100, 0, 1, 4, 1),
('pg005', '1', '满 30 减 10 券',    '兑换后自动发放到账户', '🎫', 200, -1, 0, 1, 5, 1),
('pg006', '1', '手工水饺（一份）',  '凭核销码到店兑换', '🥟', 180, 30, 0, 1, 6, 1);

-- ------------------------------------------------------------
-- 积分兑换记录表
-- 状态 0待发放 1已发放 2已核销
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `points_exchange`;
CREATE TABLE `points_exchange` (
  `id` varchar(128) NOT NULL COMMENT '主键ID',
  `shop_id` varchar(128) DEFAULT NULL COMMENT '所属店铺ID（多店铺隔离）',
  `exchange_no` varchar(50) NOT NULL COMMENT '兑换单号',
  `user_id` varchar(128) NOT NULL COMMENT '用户ID',
  `goods_id` varchar(128) NOT NULL COMMENT '积分商品ID',
  `goods_name` varchar(100) NOT NULL COMMENT '商品名称(快照)',
  `goods_image` varchar(500) DEFAULT NULL COMMENT '商品图片(快照)',
  `points` int NOT NULL COMMENT '消耗积分',
  `quantity` int DEFAULT '1' COMMENT '数量',
  `status` tinyint DEFAULT '0' COMMENT '状态 0待发放 1已发放 2已核销',
  `verify_code` varchar(20) DEFAULT NULL COMMENT '核销码',
  `verify_time` datetime DEFAULT NULL COMMENT '核销时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exchange_no` (`exchange_no`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_verify_code` (`verify_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='积分兑换记录表';

SET FOREIGN_KEY_CHECKS = 1;
