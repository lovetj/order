package com.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.order.common.Result;
import com.order.entity.PointsGoods;
import com.order.service.PointsService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 积分商城管理 —— 店家端（仅本店）
 */
@RestController
@RequestMapping("/api/points/admin")
public class PointsAdminController {

    @Autowired
    private PointsService pointsService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 积分商品列表（含下架，仅本店） */
    @GetMapping("/list")
    public Result<List<PointsGoods>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(pointsService.listForAdmin(shopId));
    }

    /** 新增积分商品 */
    @PostMapping
    public Result<Void> add(@RequestBody PointsGoods goods,
                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        goods.setShopId(shopId);
        if (goods.getStatus() == null) {
            goods.setStatus(1);
        }
        if (goods.getStock() == null) {
            goods.setStock(-1);
        }
        if (goods.getExchangedCount() == null) {
            goods.setExchangedCount(0);
        }
        if (goods.getPerLimit() == null) {
            goods.setPerLimit(1);
        }
        if (goods.getIsDel() == null) {
            goods.setIsDel(0);
        }
        pointsService.save(goods);
        return Result.success();
    }

    /** 编辑积分商品（校验归属店铺） */
    @PutMapping
    public Result<Void> update(@RequestBody PointsGoods goods,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (goods.getId() == null) {
            return Result.error("商品ID不能为空");
        }
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        PointsGoods exist = pointsService.getById(goods.getId());
        if (exist == null) {
            return Result.error(404, "商品不存在");
        }
        if (shopId == null || !shopId.equals(exist.getShopId())) {
            return Result.error(403, "无权操作其他店铺的商品");
        }
        goods.setShopId(exist.getShopId());
        pointsService.updateById(goods);
        return Result.success();
    }

    /** 上下架 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam Integer status,
                                     @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        PointsGoods goods = pointsService.getById(id);
        if (goods == null) {
            return Result.error(404, "商品不存在");
        }
        if (shopId == null || !shopId.equals(goods.getShopId())) {
            return Result.error(403, "无权操作其他店铺的商品");
        }
        goods.setStatus(status);
        pointsService.updateById(goods);
        return Result.success();
    }

    /** 删除（逻辑删除，校验归属店铺） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        PointsGoods goods = pointsService.getById(id);
        if (goods == null) {
            return Result.error(404, "商品不存在");
        }
        if (shopId == null || !shopId.equals(goods.getShopId())) {
            return Result.error(403, "无权操作其他店铺的商品");
        }
        goods.setIsDel(1);
        pointsService.updateById(goods);
        return Result.success();
    }
}
