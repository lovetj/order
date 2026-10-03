package com.order.service;

import com.order.dto.ReportVO;

public interface ReportService {

    /**
     * 经营报表（仅统计指定店铺）
     *
     * @param shopId    店铺ID
     * @param startDate 开始日期 yyyy-MM-dd，为空默认近 7 天
     * @param endDate   结束日期 yyyy-MM-dd，为空默认今天
     */
    ReportVO getReport(String shopId, String startDate, String endDate);
}
