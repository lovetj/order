package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 小票数据 VO —— 打印小票所需全部信息
 */
@Data
public class ReceiptVO {
    /** 门店信息 */
    private String shopName;
    private String shopPhone;
    private String shopAddress;

    /** 订单信息 */
    private String orderNo;
    /** 取餐号（订单号后 4 位） */
    private String pickNo;
    private String tableNo;
    private Integer peopleCount;
    private String createTime;
    private String remark;
    private String statusText;

    /** 明细 */
    private List<ReceiptItem> items;

    /** 金额 */
    private BigDecimal productTotal;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;

    /** 纯文本小票（供直接打印/预览） */
    private String text;

    @Data
    public static class ReceiptItem {
        private String name;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal amount;
        /** 规格描述，如「微辣, 加蛋」 */
        private String specText;

        public ReceiptItem() {
        }

        public ReceiptItem(String name, Integer quantity, BigDecimal price, BigDecimal amount) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
            this.amount = amount;
        }

        public ReceiptItem(String name, Integer quantity, BigDecimal price, BigDecimal amount, String specText) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
            this.amount = amount;
            this.specText = specText;
        }
    }
}
