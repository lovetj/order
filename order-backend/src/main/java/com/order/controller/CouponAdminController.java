package com.order.controller;

import com.order.common.Result;
import com.order.entity.Coupon;
import com.order.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券管理 —— 店家端
 */
@RestController
@RequestMapping("/api/coupon/admin")
public class CouponAdminController {

    @Autowired
    private CouponService couponService;

    /** 优惠券列表 */
    @GetMapping("/list")
    public Result<List<Coupon>> list() {
        return Result.success(couponService.list());
    }

    /** 新增优惠券 */
    @PostMapping
    public Result<Void> add(@RequestBody Coupon coupon) {
        if (coupon.getStatus() == null) {
            coupon.setStatus(1);
        }
        if (coupon.getIssuedCount() == null) {
            coupon.setIssuedCount(0);
        }
        if (coupon.getTotalCount() == null) {
            coupon.setTotalCount(-1);
        }
        if (coupon.getPerLimit() == null) {
            coupon.setPerLimit(1);
        }
        if (coupon.getType() == null) {
            coupon.setType(1);
        }
        couponService.save(coupon);
        return Result.success();
    }

    /** 编辑优惠券 */
    @PutMapping
    public Result<Void> update(@RequestBody Coupon coupon) {
        if (coupon.getId() == null) {
            return Result.error("优惠券ID不能为空");
        }
        couponService.updateById(coupon);
        return Result.success();
    }

    /** 启用/停用 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        Coupon coupon = couponService.getById(id);
        if (coupon == null) {
            return Result.error(404, "优惠券不存在");
        }
        coupon.setStatus(status);
        couponService.updateById(coupon);
        return Result.success();
    }

    /** 删除优惠券 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        couponService.removeById(id);
        return Result.success();
    }
}
