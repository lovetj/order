package com.order.controller;

import com.order.common.Result;
import com.order.entity.DiningTable;
import com.order.service.DiningTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 餐桌 —— 顾客端公开查询（供顾客扫码进入点餐页后，根据桌位ID回查桌号等展示信息）。
 * 独立于 /api/table/**（该前缀由店家鉴权拦截），本路径默认放行，无需登录。
 */
@RestController
@RequestMapping("/api/dining-table")
public class DiningTablePublicController {

    @Autowired
    private DiningTableService diningTableService;

    /** 根据桌位ID查询桌位公开信息（楼号、类型、别名、桌号、容量、是否启用） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getTable(@PathVariable String id) {
        DiningTable table = diningTableService.getById(id);
        if (table == null) {
            return Result.error(404, "桌位不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", table.getId());
        data.put("shopId", table.getShopId());
        data.put("buildingNo", table.getBuildingNo() != null ? table.getBuildingNo() : "1楼");
        data.put("type", table.getType() != null ? table.getType() : "大厅");
        data.put("alias", table.getAlias());
        data.put("tableNo", table.getTableNo());
        data.put("capacity", table.getCapacity());
        data.put("status", table.getStatus());
        return Result.success(data);
    }
}
