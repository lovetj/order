package com.order.controller;

import com.order.common.Result;
import com.order.service.CustomerSessionService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 顾客三要素登录态（user + shop + table）
 * 扫码/换桌时调用 /bind 刷新 Redis 会话；/check 校验当前登录态；/logout 清除当前(店铺,桌位)会话。
 * 商家端仍走 JWT，不受影响。
 */
@RestController
@RequestMapping("/api/customer/session")
public class CustomerSessionController {

    @Autowired
    private CustomerSessionService customerSessionService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 扫码/换桌统一登录态：为当前登录用户重建/刷新 (user, shop, table) 会话
     * body: { shopId, tableId }
     * 未登录（token 失效）返回 401，前端据此重新登录。
     */
    @PostMapping("/bind")
    public Result<Map<String, Object>> bind(@RequestBody Map<String, Object> body,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String userId = resolveAuthenticatedUserId(authorization);
        if (userId == null) {
            return Result.error(401, "登录已过期，请重新登录");
        }
        String shopId = body.get("shopId") == null ? null : String.valueOf(body.get("shopId"));
        String tableId = body.get("tableId") == null ? null : String.valueOf(body.get("tableId"));
        if (!customerSessionService.isEligible(userId, shopId, tableId)) {
            return Result.error(400, "缺少店铺或桌位信息，请重新扫描桌位二维码");
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
        customerSessionService.bindLogin(userId, shopId, tableId, token);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("expireSeconds", customerSessionService.getExpireSeconds());
        return Result.success(result);
    }

    /**
     * 校验当前三要素登录态；有效返回 ok，无效返回 401
     */
    @GetMapping("/check")
    public Result<Void> check(@RequestHeader(value = "Authorization", required = false) String authorization,
                              @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String shopId,
                              @RequestHeader(value = "X-Table-Id", required = false) String tableId) {
        String userId = resolveAuthenticatedUserId(authorization);
        if (userId == null || !customerSessionService.validateLogin(userId, shopId, tableId, extractToken(authorization))) {
            return Result.error(401, "登录已过期，请重新登录");
        }
        return Result.success();
    }

    /**
     * 退出登录：删除当前用户在当前(店铺,桌位)下的 Redis 会话，之后需重新登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String shopId,
                               @RequestHeader(value = "X-Table-Id", required = false) String tableId) {
        String userId = resolveAuthenticatedUserId(authorization);
        if (userId == null) {
            return Result.success();
        }
        customerSessionService.removeLogin(userId, shopId, tableId);
        return Result.success();
    }

    private String resolveAuthenticatedUserId(String authorization) {
        if (StringUtils.hasText(authorization) && jwtUtil.validateToken(authorization)) {
            return jwtUtil.getUserId(authorization);
        }
        return null;
    }

    private String extractToken(String authorization) {
        if (StringUtils.hasText(authorization)) {
            return authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
        }
        return null;
    }
}