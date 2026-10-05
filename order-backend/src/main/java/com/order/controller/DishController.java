package com.order.controller;

import com.order.common.PageResult;
import com.order.common.Result;
import com.order.dto.DishDTO;
import com.order.dto.DishVO;
import com.order.dto.DishStatsVO;
import com.order.dto.PageDTO;
import com.order.service.DishService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 菜品 —— 顾客端点餐 / 店家端商品管理（按店铺隔离）
 */
@RestController
@RequestMapping("/api/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private JwtUtil jwtUtil;

    // ==================== 顾客端 ====================

    /** 顾客端：菜品列表（仅上架，限当前店铺） */
    @GetMapping("/list")
    public Result<List<DishVO>> list(@RequestParam(required = false) String categoryId,
                                     @RequestParam(required = false) String keyword,
                                     @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String shopId = ShopContext.resolveCustomerShopId(headerShopId);
        return Result.success(dishService.listForCustomer(shopId, categoryId, keyword));
    }

    /** 顾客端：首页店长推荐（热销，限当前店铺） */
    @GetMapping("/recommend")
    public Result<List<DishVO>> recommend(@RequestParam(defaultValue = "4") Integer limit,
                                          @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String shopId = ShopContext.resolveCustomerShopId(headerShopId);
        List<DishVO> list = dishService.listForCustomer(shopId, null, null);
        if (list.size() > limit) {
            list = list.subList(0, limit);
        }
        return Result.success(list);
    }

    /** 菜品详情 */
    @GetMapping("/{id}")
    public Result<DishVO> detail(@PathVariable String id) {
        DishVO vo = dishService.getDetail(id);
        if (vo == null) {
            return Result.error(404, "菜品不存在");
        }
        return Result.success(vo);
    }

    // ==================== 店家端 ====================

    /** 管理端：菜品分页列表（含下架，仅本店） */
    @PostMapping("/page")
    public Result<PageResult<DishVO>> page(@RequestBody(required = false) PageDTO pageDTO,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(dishService.pageForAdmin(shopId, pageDTO));
    }

    /** 管理端：菜品列表（仅本店） */
    @GetMapping("/admin/list")
    public Result<List<DishVO>> adminList(@RequestParam(required = false) String categoryId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(dishService.listForAdmin(shopId, categoryId, keyword, status));
    }

    /** 管理端：新增菜品（返回新菜品ID） */
    @PostMapping
    public Result<String> add(@Valid @RequestBody DishDTO dto,
                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(dishService.addDish(dto, shopId));
    }

    /** 管理端：编辑菜品 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody DishDTO dto,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        dishService.updateDish(dto, shopId);
        return Result.success();
    }

    /** 管理端：上下架 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam Integer status,
                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        dishService.updateStatus(id, status, shopId);
        return Result.success();
    }

    /** 管理端：改价（只改价格） */
    @PutMapping("/{id}/price")
    public Result<Void> updatePrice(@PathVariable String id, @RequestParam java.math.BigDecimal price,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        DishDTO dto = new DishDTO();
        dto.setId(id);
        dto.setPrice(price);
        dishService.updateDish(dto, shopId);
        return Result.success();
    }

    /** 管理端：删除菜品 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        dishService.deleteDish(id, shopId);
        return Result.success();
    }

    /** 管理端：批量删除 */
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<String> ids,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        dishService.deleteBatch(ids, shopId);
        return Result.success();
    }

    /** 管理端：商品统计（总数 / 在售 / 各分类） */
    @GetMapping("/admin/stats")
    public Result<DishStatsVO> stats(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(dishService.statsForAdmin(shopId));
    }
}