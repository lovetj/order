package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 店家端管理后台看板 VO —— 与前端 dashboard 页面字段对齐
 */
@Data
public class DashboardVO {

    /** 数据概览卡片 */
    private List<OverviewItem> overview;

    /** 近 7 日营业额柱状图 */
    private List<WeekSale> weekSales;

    /** 待处理订单 */
    private List<OrderVO> pendingOrders;

    @Data
    public static class OverviewItem {
        private String label;
        private String value;
        private String extra;
        /** primary / blue / green / orange */
        private String color;

        public OverviewItem() {
        }

        public OverviewItem(String label, String value, String extra, String color) {
            this.label = label;
            this.value = value;
            this.extra = extra;
            this.color = color;
        }
    }

    @Data
    public static class WeekSale {
        private String day;
        private BigDecimal value;
        /** 相对最大值百分比，用于柱状图高度 */
        private Integer percent;

        public WeekSale() {
        }

        public WeekSale(String day, BigDecimal value, Integer percent) {
            this.day = day;
            this.value = value;
            this.percent = percent;
        }
    }
}
