package com.order.controller;

import com.order.common.Result;
import com.order.dto.TableTransferDTO;
import com.order.entity.DiningTable;
import com.order.service.DiningTableService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 餐桌 —— 店家端桌位与二维码管理（店铺ID来自登录态，不信任前端参数）
 */
@RestController
@RequestMapping("/api/table")
public class DiningTableController {

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 桌位列表（仅本店） */
    @GetMapping("/list")
    public Result<List<DiningTable>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(diningTableService.listByShop(shopId));
    }

    /** 新增桌位 */
    @PostMapping
    public Result<Void> add(@RequestBody DiningTable table,
                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        diningTableService.addTable(table, shopId);
        return Result.success();
    }

    /** 编辑桌位 */
    @PutMapping
    public Result<Void> update(@RequestBody DiningTable table,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        diningTableService.updateTable(table, shopId);
        return Result.success();
    }

    /** 启用/停用切换 */
    @PutMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable String id,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        diningTableService.toggleStatus(id, shopId);
        return Result.success();
    }

    /** 删除桌位 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id,
                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        diningTableService.deleteTable(id, shopId);
        return Result.success();
    }

    /** 清台 */
    @PutMapping("/{id}/clean")
    public Result<Void> clean(@PathVariable String id,
                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        diningTableService.cleanTable(id, shopId);
        return Result.success();
    }

    /** 拼桌设置 */
    @PutMapping("/{id}/share")
    public Result<Void> share(@PathVariable String id,
                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        diningTableService.shareTable(id, shopId);
        return Result.success();
    }

    /** 换桌操作 */
    @PostMapping("/transfer")
    public Result<Void> transfer(@Valid @RequestBody TableTransferDTO dto,
                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        diningTableService.transferTable(dto, shopId);
        return Result.success();
    }
}
