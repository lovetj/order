package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表（堂食扫码点餐）
 */
@Data
@TableName("`order`")
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 订单号，如 D20261003001 */
    private String orderNo;

    private String userId;

    private String shopId;

    /** 桌号，如 A01 */
    private String tableNo;

    /** 就餐方式 1堂食 2外带 */
    private Integer diningType;

    /** 就餐人数 */
    private Integer peopleCount;

    private BigDecimal productTotal;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 支付方式 1微信线上 2支付宝线上 3线下微信 4线下支付宝 5线下其他 99未支付 */
    private Integer payType;

    /** 支付大类 1线上支付 2线下支付 */
    private Integer payMode;

    /** 支付状态 0待支付 1支付中 2已支付 3支付失败/已关闭 */
    private Integer payStatus;

    /** 线下支付收款二维码快照URL */
    private String payQrcode;

    /** 关联支付流水号 */
    private String paymentNo;

    /**
     * 状态：0待接单 1制作中 2已完成 3已取消
     * 前端 status: pending / cooking / done / canceled
     */
    private Integer status;

    private String remark;

    private LocalDateTime payTime;

    /** 线下支付店员确认到账时间 */
    private LocalDateTime offlinePayTime;

    /** 线下支付确认操作人/店员ID */
    private String offlinePayOperator;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    private String cancelReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
