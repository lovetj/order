package com.order.util;

import org.springframework.util.StringUtils;

/**
 * 鉴权辅助工具
 *
 * 统一从请求头解析当前登录用户/角色，避免各 Controller 重复实现。
 * 兼容两种传递方式：
 *   1) Authorization: Bearer {token}  （优先，前端默认方式）
 *   2) userId: {userId}               （本地调试用）
 */
public final class AuthUtil {

    private AuthUtil() {
    }

    /** 解析当前登录用户ID，未登录返回 null */
    public static String resolveUserId(JwtUtil jwtUtil, String authorization, String headerUserId) {
        if (StringUtils.hasText(authorization) && jwtUtil.validateToken(authorization)) {
            return jwtUtil.getUserId(authorization);
        }
        if (StringUtils.hasText(headerUserId)) {
            return headerUserId.trim();
        }
        return null;
    }

    /** 解析当前登录角色（customer / merchant），无法解析返回 null */
    public static String resolveRole(JwtUtil jwtUtil, String authorization) {
        if (StringUtils.hasText(authorization) && jwtUtil.validateToken(authorization)) {
            return jwtUtil.getRole(authorization);
        }
        return null;
    }

    /**
     * 校验是否为店家角色
     * 说明：店家端接口（商品/订单/桌位/优惠券/积分商品/报表等写操作）应调用此方法。
     */
    public static boolean isMerchant(JwtUtil jwtUtil, String authorization) {
        return "merchant".equals(resolveRole(jwtUtil, authorization));
    }
}
