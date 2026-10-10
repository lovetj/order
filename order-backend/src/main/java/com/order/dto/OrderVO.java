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
    /** 就餐方式 1堂食 2外带 */
    private Integer diningType;
    /** 就餐方式文案：堂食/外带 */
    private String diningTypeText;
    /** 前端使用字符串状态：pending / cooking / done / canceled */
    private String status;
    /** 前端使用中文状态文案 */
    private String statusText;

    /** 支付方式 1微信线上 2支付宝线上 3线下微信 4线下支付宝 5线下其他 99未支付 */
    private Integer payType;
    /** 支付方式文案 */
    private String payTypeText;
    /** 支付大类 1线上支付 2线下支付 */
    private Integer payMode;
    /** 支付状态 0待支付 1支付中 2已支付 3支付失败/已关闭 */
    private Integer payStatus;
    /** 支付状态文案 */
    private String payStatusText;
    /** 线下支付收款二维码快照URL */
    private String payQrcode;
    /** 线下支付 - 微信收款码 */
    private String offlineWxQrcode;
    /** 线下支付 - 支付宝收款码 */
    private String offlineAlipayQrcode;
    /** 线下支付 - 其他收款码 */
    private String offlineOtherQrcode;
    /** 关联支付流水号 */
    private String paymentNo;
    /** 支付完成时间 */
    private String payTime;
    /** 线下确认到账时间 */
    private String offlinePayTime;

    private String createTime;
    private BigDecimal amount;
    private BigDecimal productTotal;
    private Integer peopleCount;
    private String remark;
    /** 用户手机号 */
    private String phone;
    private String userPhone;
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
