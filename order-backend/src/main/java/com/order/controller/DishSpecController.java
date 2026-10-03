package com.order.controller;

import com.order.common.Result;
import com.order.dto.DishSpecVO;
import com.order.entity.DishSpec;
import com.order.service.DishSpecService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品规格/口味 —— 顾客端选择 + 店家端维护（按店铺隔离）
 */
@RestController
@RequestMapping("/api/dish-spec")
public class DishSpecController {

    @Autowired
    private DishSpecService dishSpecService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 顾客端：某菜品的规格分组（辣度/加料等） */
    @GetMapping("/{dishId}")
    public Result<List<DishSpecVO>> listGrouped(@PathVariable String dishId) {
        return Result.success(dishSpecService.listGroupedByDish(dishId));
    }

    /** 店家端：某菜品的规格明细 */
    @GetMapping("/{dishId}/detail")
    public Result<List<DishSpec>> listDetail(@PathVariable String dishId) {
        return Result.success(dishSpecService.listByDish(dishId));
    }

    /** 店家端：整体保存某菜品的规格（店铺ID来自登录态） */
    @PostMapping("/{dishId}")
    public Result<Void> save(@PathVariable String dishId, @RequestBody List<DishSpec> specs,
                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        dishSpecService.saveSpecs(dishId, specs, shopId);
        return Result.success();
    }
}
