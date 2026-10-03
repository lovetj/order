package com.order.controller;

import com.order.common.PageResult;
import com.order.common.Result;
import com.order.dto.OrderCreateDTO;
import com.order.dto.OrderVO;
import com.order.service.OrderService;
import com.order.util.AuthUtil;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 订单 —— 顾客端下单/查询 + 店家端处理（按店铺隔离）
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    // ==================== 顾客端 ====================

    /** 顾客下单（店铺来自扫码携带的 X-Shop-Id） */
    @PostMapping("/create")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto,
                                  @RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestHeader(value = "userId", required = false) String headerUserId,
                                  @RequestHeader(value = ShopContext.SHOP_ID_HEADER, required = false) String headerShopId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        String shopId = ShopContext.resolveCustomerShopId(headerShopId);
        if (shopId == null) {
            return Result.error("未识别店铺信息，请重新扫描桌位二维码");
        }
        return Result.success(orderService.createOrder(dto, userId, shopId));
    }

    /** 顾客订单分页列表（按用户维度，跨店铺） */
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String status,
                                            @RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestHeader(value = "userId", required = false) String headerUserId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        return Result.success(orderService.pageForCustomer(pageNum, pageSize, status, userId));
    }

    /** 顾客订单各状态数量（"我的"页入口/订单页签） */
    @GetMapping("/counts")
    public Result<Map<String, Long>> counts(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestHeader(value = "userId", required = false) String headerUserId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        return Result.success(orderService.countByUser(userId));
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable String id) {
        OrderVO vo = orderService.getDetail(id);
        if (vo == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success(vo);
    }

    /** 顾客取消订单 */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable String id,
                               @RequestParam(required = false) String reason,
                               @RequestHeader(value = "Authorization", required = false) String authorization,
                               @RequestHeader(value = "userId", required = false) String headerUserId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        orderService.cancel(id, userId, reason);
        return Result.success();
    }

    // ==================== 店家端（仅本店） ====================

    /** 店家订单分页列表（仅本店） */
    @GetMapping("/admin/list")
    public Result<PageResult<OrderVO>> adminList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String status,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(orderService.pageForMerchant(shopId, pageNum, pageSize, status));
    }

    /** 店家订单各状态数量（Tab 角标，仅本店） */
    @GetMapping("/admin/counts")
    public Result<Map<String, Long>> adminCounts(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(orderService.countForMerchant(shopId));
    }

    /** 店家接单 */
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable String id,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        orderService.accept(id, shopId);
        return Result.success();
    }

    /** 店家出餐完成 */
    @PutMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable String id,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        orderService.finish(id, shopId);
        return Result.success();
    }

    /** 店家拒单 */
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable String id,
                               @RequestParam(required = false) String reason,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        orderService.reject(id, reason, shopId);
        return Result.success();
    }
}
