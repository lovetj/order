package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单 VO —— 字段与前端订单卡片完全对齐
 * { id, table, status, statusText, createTime, amount, items:[{name,count,price}] }
 */
@Data
public class OrderVO {
    private String id;
    private String orderNo;
    /** 前端使用 table */
    private String table;
    /** 前端使用字符串状态：pending / cooking / done / canceled */
    private String status;
    /** 前端使用中文状态文案 */
    private String statusText;
    private String createTime;
    private BigDecimal amount;
    private BigDecimal productTotal;
    private Integer peopleCount;
    private String remark;
    /** 管理端使用的下一步操作：{ text, next, nextText } */
    private Action action;
    private List<OrderItemVO> items;

    @Data
    public static class Action {
        private String text;
        private String next;
        private String nextText;

        public Action() {
        }

        public Action(String text, String next, String nextText) {
            this.text = text;
            this.next = next;
            this.nextText = nextText;
        }
    }
}
