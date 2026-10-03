package com.order.service;

/**
 * 报表导出（均按店铺隔离）
 */
public interface ExportService {

    /**
     * 导出经营报表为 CSV（UTF-8 BOM，Excel 可直接打开）
     *
     * @param shopId    店铺ID
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @return CSV 文本内容
     */
    String exportReportCsv(String shopId, String startDate, String endDate);

    /**
     * 导出订单明细为 CSV（仅本店）
     */
    String exportOrdersCsv(String shopId, String startDate, String endDate, Integer status);
}
