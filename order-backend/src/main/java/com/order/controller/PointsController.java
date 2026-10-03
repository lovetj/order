package com.order.controller;

import com.order.common.Result;
import com.order.entity.PointsExchange;
import com.order.entity.PointsGoods;
import com.order.service.PointsService;
import com.order.util.AuthUtil;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 积分商城 —— 顾客端兑换 + 店家端核销（按店铺隔离）
 */
@RestController
@RequestMapping("/api/points")
public class PointsController {

    @Autowired
    private PointsService pointsService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 积分商城商品列表（按当前店铺） */
    @GetMapping("/goods")
    public Result<List<PointsGoods>> goods(
            @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        return Result.success(pointsService.listAvailable(ShopContext.resolveCustomerShopId(headerShopId)));
    }

    /** 积分兑换 */
    @PostMapping("/exchange")
    public Result<PointsExchange> exchange(@RequestBody Map<String, Object> body,
                                           @RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestHeader(value = "userId", required = false) String headerUserId,
                                           @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        String goodsId = body.get("goodsId") == null ? null : String.valueOf(body.get("goodsId"));
        Integer quantity = body.get("quantity") == null ? 1 : Integer.valueOf(String.valueOf(body.get("quantity")));
        if (goodsId == null || goodsId.isEmpty()) {
            return Result.error("商品ID不能为空");
        }
        return Result.success(pointsService.exchange(goodsId, quantity, userId, ShopContext.resolveCustomerShopId(headerShopId)));
    }

    /** 我的兑换记录（可按店铺过滤） */
    @GetMapping("/mine")
    public Result<List<PointsExchange>> mine(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestHeader(value = "userId", required = false) String headerUserId,
                                             @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        return Result.success(pointsService.listMyExchanges(userId, ShopContext.resolveCustomerShopId(headerShopId)));
    }

    /** 店家端：核销（仅限本店兑换记录） */
    @PostMapping("/verify")
    public Result<Void> verify(@RequestBody Map<String, String> body,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!AuthUtil.isMerchant(jwtUtil, authorization)) {
            return Result.error(403, "无权限操作，请以店家身份登录");
        }
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        pointsService.verify(body.get("verifyCode"), shopId);
        return Result.success();
    }
}
