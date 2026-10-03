package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户优惠券 VO —— 字段对齐前端展示
 */
@Data
public class UserCouponVO {
    private String id;
    private String couponId;
    private String name;
    /** 1满减 2折扣 */
    private Integer type;
    private BigDecimal threshold;
    private BigDecimal amount;
    private BigDecimal discount;
    /** 0未使用 1已使用 2已过期 */
    private Integer status;
    private String statusText;
    private String startTime;
    private String endTime;
    /** 是否可用于当前订单金额 */
    private Boolean usable;
    /** 券面展示文案，如「满 60 减 10」 */
    private String label;
}
