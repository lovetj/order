package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.order.dto.ReportVO;
import com.order.entity.Order;
import com.order.entity.OrderItem;
import com.order.mapper.OrderItemMapper;
import com.order.mapper.OrderMapper;
import com.order.service.ExportService;
import com.order.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String LINE_SEP = "\r\n";
    /** UTF-8 BOM，保证 Excel 正确识别中文 */
    private static final String BOM = "\uFEFF";

    @Autowired
    private ReportService reportService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public String exportReportCsv(String shopId, String startDate, String endDate) {
        String end = StringUtils.hasText(endDate) ? endDate : LocalDate.now().format(DATE);
        String start = StringUtils.hasText(startDate)
                ? startDate
                : LocalDate.now().minusDays(6).format(DATE);

        ReportVO report = reportService.getReport(shopId, start, end);
        StringBuilder sb = new StringBuilder(BOM);

        // 标题
        sb.append("经营报表,").append(start).append(" 至 ").append(end).append(LINE_SEP).append(LINE_SEP);

        // 汇总
        ReportVO.Summary summary = report.getSummary();
        sb.append("汇总").append(LINE_SEP);
        sb.append("营业额,订单数,客单价,已取消订单数").append(LINE_SEP);
        sb.append(csv(summary.getAmount())).append(",")
                .append(summary.getOrderCount()).append(",")
                .append(csv(summary.getAvgAmount())).append(",")
                .append(summary.getCanceledCount()).append(LINE_SEP).append(LINE_SEP);

        // 每日明细
        sb.append("每日明细").append(LINE_SEP);
        sb.append("日期,营业额,订单数").append(LINE_SEP);
        if (report.getDailyList() != null) {
            for (ReportVO.DailyItem item : report.getDailyList()) {
                sb.append(item.getDate()).append(",")
                        .append(csv(item.getAmount())).append(",")
                        .append(item.getOrderCount()).append(LINE_SEP);
            }
        }
        sb.append(LINE_SEP);

        // 菜品排行
        sb.append("菜品销量排行").append(LINE_SEP);
        sb.append("排名,菜品名称,销量,销售额").append(LINE_SEP);
        if (report.getTopDishes() != null) {
            int rank = 1;
            for (ReportVO.TopDish item : report.getTopDishes()) {
                sb.append(rank++).append(",")
                        .append(csv(item.getDishName())).append(",")
                        .append(item.getQuantity()).append(",")
                        .append(csv(item.getAmount())).append(LINE_SEP);
            }
        }
        sb.append(LINE_SEP);

        // 分类销量
        sb.append("分类销售额").append(LINE_SEP);
        sb.append("分类,销量,销售额").append(LINE_SEP);
        if (report.getCategoryList() != null) {
            for (ReportVO.CategoryItem item : report.getCategoryList()) {
                sb.append(csv(item.getCategoryName())).append(",")
                        .append(item.getQuantity()).append(",")
                        .append(csv(item.getAmount())).append(LINE_SEP);
            }
        }
        return sb.toString();
    }

    @Override
    public String exportOrdersCsv(String shopId, String startDate, String endDate, Integer status) {
        String end = StringUtils.hasText(endDate) ? endDate : LocalDate.now().format(DATE);
        String start = StringUtils.hasText(startDate)
                ? startDate
                : LocalDate.now().minusDays(6).format(DATE);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        // 店铺隔离：仅导出本店订单
        if (StringUtils.hasText(shopId)) {
            wrapper.eq(Order::getShopId, shopId);
        }
        wrapper.between(Order::getCreateTime, start + " 00:00:00", end + " 23:59:59");
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(wrapper);

        StringBuilder sb = new StringBuilder(BOM);
        sb.append("订单号,桌号,人数,菜品明细,商品小计,优惠金额,实付金额,状态,下单时间,备注").append(LINE_SEP);
        for (Order order : orders) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            StringBuilder detail = new StringBuilder();
            for (OrderItem item : items) {
                if (detail.length() > 0) {
                    detail.append(" ");
                }
                detail.append(item.getDishName()).append("x").append(item.getQuantity());
            }
            sb.append(csv(order.getOrderNo())).append(",")
                    .append(csv(order.getTableNo())).append(",")
                    .append(order.getPeopleCount() == null ? "" : order.getPeopleCount()).append(",")
                    .append(csv(detail.toString())).append(",")
                    .append(csv(order.getProductTotal())).append(",")
                    .append(csv(order.getDiscountAmount())).append(",")
                    .append(csv(order.getPayAmount())).append(",")
                    .append(csv(statusText(order.getStatus()))).append(",")
                    .append(order.getCreateTime() == null ? "" : order.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(",")
                    .append(csv(order.getRemark())).append(LINE_SEP);
        }
        return sb.toString();
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0: return "待接单";
            case 1: return "制作中";
            case 2: return "已完成";
            case 3: return "已取消";
            default: return "未知";
        }
    }

    private String csv(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** CSV 字段转义：含逗号/引号/换行时用双引号包裹 */
    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
