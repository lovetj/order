package com.order.controller;

import com.order.common.Result;
import com.order.dto.UserCouponVO;
import com.order.entity.Coupon;
import com.order.service.CouponService;
import com.order.util.AuthUtil;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 优惠券 —— 顾客端领取/查看 + 结算试算（按店铺隔离）
 */
@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 可领取的优惠券列表（按当前店铺） */
    @GetMapping("/available")
    public Result<List<Coupon>> available(
            @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        return Result.success(couponService.listAvailable(ShopContext.resolveCustomerShopId(headerShopId)));
    }

    /** 领取优惠券 */
    @PostMapping("/receive/{couponId}")
    public Result<Void> receive(@PathVariable String couponId,
                                @RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestHeader(value = "userId", required = false) String headerUserId,
                                @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        couponService.receiveCoupon(couponId, userId, ShopContext.resolveCustomerShopId(headerShopId));
        return Result.success();
    }

    /** 我的优惠券（可按店铺过滤） */
    @GetMapping("/mine")
    public Result<List<UserCouponVO>> mine(@RequestParam(required = false) Integer status,
                                           @RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestHeader(value = "userId", required = false) String headerUserId,
                                           @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        return Result.success(couponService.listMyCoupons(userId, status, ShopContext.resolveCustomerShopId(headerShopId)));
    }

    /** 我的优惠券数量统计（可按店铺过滤） */
    @GetMapping("/counts")
    public Result<Map<String, Long>> counts(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestHeader(value = "userId", required = false) String headerUserId,
                                            @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        return Result.success(couponService.countMyCoupons(userId, ShopContext.resolveCustomerShopId(headerShopId)));
    }

    /** 结算试算：给定金额返回最优可用券（限定店铺） */
    @GetMapping("/best")
    public Result<UserCouponVO> best(@RequestParam BigDecimal amount,
                                     @RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestHeader(value = "userId", required = false) String headerUserId,
                                     @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        return Result.success(couponService.findBestCoupon(userId, amount, ShopContext.resolveCustomerShopId(headerShopId)));
    }
}
