package com.order.service.impl;

import com.order.dto.ReportVO;
import com.order.mapper.ReportMapper;
import com.order.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private ReportMapper reportMapper;

    @Override
    public ReportVO getReport(String shopId, String startDate, String endDate) {
        // 默认近 7 天（含今天）
        String end = StringUtils.hasText(endDate) ? endDate : LocalDate.now().format(DATE);
        String start = StringUtils.hasText(startDate)
                ? startDate
                : LocalDate.now().minusDays(6).format(DATE);

        ReportVO vo = new ReportVO();
        vo.setStartDate(start);
        vo.setEndDate(end);

        // 1. 区间汇总（限定本店）
        Map<String, Object> range = reportMapper.rangeSummary(shopId, start, end);
        BigDecimal amount = BigDecimal.ZERO;
        long orderCount = 0L;
        if (range != null) {
            amount = toDecimal(range.get("amount"));
            orderCount = toLong(range.get("orderCount"));
        }
        BigDecimal avg = orderCount > 0
                ? amount.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 已取消订单数
        long canceledCount = 0L;
        java.util.List<Map<String, Object>> statusRows = reportMapper.statusSummary(shopId, start, end);
        for (Map<String, Object> row : statusRows) {
            Integer st = row.get("status") == null ? null : ((Number) row.get("status")).intValue();
            if (st != null && st == 3) {
                canceledCount = toLong(row.get("cnt"));
            }
        }
        vo.setSummary(new ReportVO.Summary(amount, orderCount, avg, canceledCount));

        // 2. 每日明细 + 柱状图百分比
        java.util.List<Map<String, Object>> dailyRows = reportMapper.dailySummary(shopId, start, end);
        java.util.List<ReportVO.DailyItem> dailyList = new ArrayList<>();
        BigDecimal max = BigDecimal.ZERO;
        for (Map<String, Object> row : dailyRows) {
            BigDecimal dayAmount = toDecimal(row.get("amount"));
            if (dayAmount.compareTo(max) > 0) {
                max = dayAmount;
            }
        }
        for (Map<String, Object> row : dailyRows) {
            String date = row.get("date") == null ? "" : String.valueOf(row.get("date"));
            BigDecimal dayAmount = toDecimal(row.get("amount"));
            long cnt = toLong(row.get("orderCount"));
            int percent = 0;
            if (max.compareTo(BigDecimal.ZERO) > 0) {
                percent = dayAmount.multiply(BigDecimal.valueOf(100))
                        .divide(max, 0, RoundingMode.HALF_UP).intValue();
            }
            if (percent == 0 && dayAmount.compareTo(BigDecimal.ZERO) > 0) {
                percent = 5;
            }
            dailyList.add(new ReportVO.DailyItem(date, dayAmount, cnt, percent));
        }
        vo.setDailyList(dailyList);

        // 3. 菜品销量排行 Top 10
        java.util.List<Map<String, Object>> topRows = reportMapper.topDishes(shopId, start, end, 10);
        java.util.List<ReportVO.TopDish> topDishes = new ArrayList<>();
        for (Map<String, Object> row : topRows) {
            topDishes.add(new ReportVO.TopDish(
                    row.get("dishName") == null ? "" : String.valueOf(row.get("dishName")),
                    toLong(row.get("quantity")),
                    toDecimal(row.get("amount"))
            ));
        }
        vo.setTopDishes(topDishes);

        // 4. 分类销量
        java.util.List<Map<String, Object>> categoryRows = reportMapper.categorySummary(shopId, start, end);
        java.util.List<ReportVO.CategoryItem> categoryList = new ArrayList<>();
        for (Map<String, Object> row : categoryRows) {
            categoryList.add(new ReportVO.CategoryItem(
                    row.get("categoryName") == null ? "未分类" : String.valueOf(row.get("categoryName")),
                    toLong(row.get("quantity")),
                    toDecimal(row.get("amount"))
            ));
        }
        vo.setCategoryList(categoryList);

        return vo;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
