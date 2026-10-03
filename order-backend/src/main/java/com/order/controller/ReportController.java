package com.order.controller;

import com.order.common.Result;
import com.order.dto.ReportVO;
import com.order.service.ReportService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 经营报表 —— 店家端（仅统计本店）
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 经营报表（仅本店）
     * 不传日期默认近 7 天
     */
    @GetMapping("/summary")
    public Result<ReportVO> summary(@RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (shopId == null) {
            return Result.error(403, "无法识别店铺信息，请重新登录");
        }
        return Result.success(reportService.getReport(shopId, startDate, endDate));
    }
}
