package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 经营报表 VO
 */
@Data
public class ReportVO {

    private String startDate;
    private String endDate;

    /** 区间汇总 */
    private Summary summary;

    /** 每日明细（柱状图/折线图） */
    private List<DailyItem> dailyList;

    /** 菜品销量排行 */
    private List<TopDish> topDishes;

    /** 分类销量占比 */
    private List<CategoryItem> categoryList;

    @Data
    public static class Summary {
        /** 营业额 */
        private BigDecimal amount;
        /** 订单数 */
        private Long orderCount;
        /** 客单价 */
        private BigDecimal avgAmount;
        /** 已取消订单数 */
        private Long canceledCount;

        public Summary() {
        }

        public Summary(BigDecimal amount, Long orderCount, BigDecimal avgAmount, Long canceledCount) {
            this.amount = amount;
            this.orderCount = orderCount;
            this.avgAmount = avgAmount;
            this.canceledCount = canceledCount;
        }
    }

    @Data
    public static class DailyItem {
        private String date;
        private BigDecimal amount;
        private Long orderCount;
        /** 相对最大值的百分比，用于柱状图高度 */
        private Integer percent;

        public DailyItem() {
        }

        public DailyItem(String date, BigDecimal amount, Long orderCount, Integer percent) {
            this.date = date;
            this.amount = amount;
            this.orderCount = orderCount;
            this.percent = percent;
        }
    }

    @Data
    public static class TopDish {
        private String dishName;
        private Long quantity;
        private BigDecimal amount;

        public TopDish() {
        }

        public TopDish(String dishName, Long quantity, BigDecimal amount) {
            this.dishName = dishName;
            this.quantity = quantity;
            this.amount = amount;
        }
    }

    @Data
    public static class CategoryItem {
        private String categoryName;
        private Long quantity;
        private BigDecimal amount;

        public CategoryItem() {
        }

        public CategoryItem(String categoryName, Long quantity, BigDecimal amount) {
            this.categoryName = categoryName;
            this.quantity = quantity;
            this.amount = amount;
        }
    }
}
