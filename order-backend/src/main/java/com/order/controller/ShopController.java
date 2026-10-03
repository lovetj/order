package com.order.controller;

import com.order.common.Result;
import com.order.dto.DashboardVO;
import com.order.entity.Shop;
import com.order.service.ShopService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 门店 —— 顾客端首页信息 / 店家端管理后台（按店铺隔离）
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 顾客端：门店信息（来自扫码携带的 X-Shop-Id） */
    @GetMapping("/info")
    public Result<Shop> info(@RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        return Result.success(shopService.getShop(ShopContext.resolveCustomerShopId(headerShopId)));
    }

    /** 店家端：管理后台看板（仅本店） */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(shopService.getDashboard(shopId));
    }

    /**
     * 店家端：获取本店信息（从登录 token 解析店铺ID，保证拿到真实数据）
     * 与顾客端 /info 区分：不依赖 X-Shop-Id，也不会返回假数据兜底。
     */
    @GetMapping("/merchant")
    public Result<Shop> merchantInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(shopService.getShopForMerchant(shopId));
    }

    /** 店家端：切换营业状态（仅本店） */
    @PutMapping("/toggle-business")
    public Result<Void> toggleBusiness(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        shopService.toggleBusinessStatus(shopId);
        return Result.success();
    }

    /** 店家端：更新门店信息（强制使用登录态店铺ID，防止篡改） */
    @PutMapping
    public Result<Void> update(@RequestBody Shop shop,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        shopService.updateShopInfo(shopId, shop);
        return Result.success();
    }
}
