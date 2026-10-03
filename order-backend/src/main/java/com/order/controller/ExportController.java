package com.order.controller;

import com.order.service.ExportService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 报表导出 —— 店家端（仅导出本店数据）
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 导出经营报表 CSV（仅本店） */
    @GetMapping("/report")
    public ResponseEntity<byte[]> exportReport(@RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate,
                                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        String csv = exportService.exportReportCsv(shopId, startDate, endDate);
        String fileName = "经营报表_" + orToday(startDate) + "_" + orToday(endDate) + ".csv";
        return buildCsvResponse(csv, fileName);
    }

    /** 导出订单明细 CSV（仅本店） */
    @GetMapping("/orders")
    public ResponseEntity<byte[]> exportOrders(@RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate,
                                               @RequestParam(required = false) Integer status,
                                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        String csv = exportService.exportOrdersCsv(shopId, startDate, endDate, status);
        String fileName = "订单明细_" + orToday(startDate) + "_" + orToday(endDate) + ".csv";
        return buildCsvResponse(csv, fileName);
    }

    private ResponseEntity<byte[]> buildCsvResponse(String csv, String fileName) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        String encodedName;
        try {
            encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            encodedName = "export.csv";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", encodedName);
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String orToday(String date) {
        return (date == null || date.trim().isEmpty()) ? LocalDate.now().toString() : date;
    }
}
