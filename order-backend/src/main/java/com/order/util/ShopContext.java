package com.order.util;

import org.springframework.util.StringUtils;

/**
 * 多店铺上下文解析工具
 *
 * 系统支持 N 个店家同时使用，所有业务数据必须按 shopId 隔离。
 * shopId 的来源分两类：
 *   1) 店家端：来自 JWT token 中的 shopId claim（登录时由 admin.shopId 写入），
 *      这是唯一可信来源，绝不信任前端传参，防止 A 店操作 B 店数据。
 *   2) 顾客端：来自扫码进入时前端携带的请求头 X-Shop-Id（顾客没有 token 绑定店铺）。
 *
 * 约定：顾客端未携带 shopId 时视为非法请求，由调用方决定是报错还是回退默认店铺。
 */
public final class ShopContext {

    /** 顾客端携带店铺ID的请求头 */
    public static final String SHOP_ID_HEADER = "X-Shop-Id";

    private ShopContext() {
    }

    /**
     * 解析店家端店铺ID（仅从 token 解析，不信任前端参数）
     *
     * @return shopId，无法解析返回 null
     */
    public static String resolveMerchantShopId(JwtUtil jwtUtil, String authorization) {
        if (jwtUtil == null || !StringUtils.hasText(authorization)) {
            return null;
        }
        if (!jwtUtil.validateToken(authorization)) {
            return null;
        }
        String shopId = jwtUtil.getShopId(authorization);
        return StringUtils.hasText(shopId) ? shopId.trim() : null;
    }

    /**
     * 解析顾客端店铺ID：优先取请求头 X-Shop-Id
     *
     * @return shopId，未携带返回 null
     */
    public static String resolveCustomerShopId(String headerShopId) {
        return StringUtils.hasText(headerShopId) ? headerShopId.trim() : null;
    }
}
