package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户已领取的优惠券
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属店铺ID（多店铺隔离） */
    private String shopId;

    private String userId;

    private String couponId;

    /** 券名称（快照） */
    private String couponName;

    /** 类型 1满减 2折扣（快照） */
    private Integer type;

    private BigDecimal threshold;

    private BigDecimal amount;

    private BigDecimal discount;

    /**
     * 状态：0未使用 1已使用 2已过期
     */
    private Integer status;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 过期时间 */
    private LocalDateTime endTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 关联订单ID */
    private String orderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
