package com.order.controller;

import com.order.common.Result;
import com.order.entity.Category;
import com.order.service.CategoryService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品分类 —— 顾客端/店家端共用（按店铺隔离）
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 顾客端：启用中的分类（按当前店铺） */
    @GetMapping("/list")
    public Result<List<Category>> list(
            @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        return Result.success(categoryService.listEnabled(ShopContext.resolveCustomerShopId(headerShopId)));
    }

    /** 管理端：全部分类（仅本店） */
    @GetMapping("/all")
    public Result<List<Category>> all(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(categoryService.listAll(ShopContext.resolveMerchantShopId(jwtUtil, authorization)));
    }

    @GetMapping("/{id}")
    public Result<Category> detail(@PathVariable String id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Category category,
                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        categoryService.addCategory(category, shopId);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Category category,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        categoryService.updateCategory(category, shopId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        categoryService.deleteCategory(id, shopId);
        return Result.success();
    }
}
