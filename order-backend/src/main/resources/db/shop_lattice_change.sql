-- ============================================================
-- shop 表新增 店铺经纬度 字段（地图选点坐标存储）
-- 用途：用于存储店家在 "店铺信息-地图选点" 选择的经纬度，
--       后端 Shop 实体 / MyBatis-Plus 自动映射 shop.latitude / shop.longitude。
-- 说明：脚本为幂等编写（列已存在时不会报错，可直接重复执行）。
-- ============================================================

-- 1) 校验当前是否存在经纬度列（返回空 = 需要补充列，正常情况下应为空）
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'shop'
  AND COLUMN_NAME IN ('latitude', 'longitude')
ORDER BY ORDINAL_POSITION;

-- 2) 幂等补充 latitude 列（不存在才 ADD）
SET @ddl1 := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop' AND COLUMN_NAME = 'latitude') = 0,
  'ALTER TABLE shop ADD COLUMN latitude DECIMAL(10,6) NULL DEFAULT NULL COMMENT ''店铺纬度（地图选点）'' AFTER address',
  'SELECT 1 AS noop');
PREPARE s1 FROM @ddl1; EXECUTE s1; DEALLOCATE PREPARE s1;

-- 3) 幂等补充 longitude 列（不存在才 ADD）
SET @ddl2 := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop' AND COLUMN_NAME = 'longitude') = 0,
  'ALTER TABLE shop ADD COLUMN longitude DECIMAL(10,6) NULL DEFAULT NULL COMMENT ''店铺经度（地图选点）'' AFTER latitude',
  'SELECT 1 AS noop');
PREPARE s2 FROM @ddl2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- 4) 如后续需要按距离查询/开放"附近门店"，可开启下方坐标索引（选点场景可选）
-- ALTER TABLE shop ADD INDEX idx_shop_location (latitude, longitude);

-- 执行后复查确认两个字段已存在
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shop'
  AND COLUMN_NAME IN ('latitude', 'longitude')
ORDER BY ORDINAL_POSITION;