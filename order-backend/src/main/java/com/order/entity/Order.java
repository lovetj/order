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

    /** 支付方式 1微信 2支付宝 99未支付 */
    private Integer payType;

    /**
     * 状态：0待接单 1制作中 2已完成 3已取消
     * 前端 status: pending / cooking / done / canceled
     */
    private Integer status;

    private String remark;

    private LocalDateTime payTime;

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
