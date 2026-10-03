package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板表
 */
@Data
@TableName("coupon")
public class Coupon implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属店铺ID（多店铺隔离） */
    private String shopId;

    private String name;

    /** 类型 1满减 2折扣 */
    private Integer type;

    /** 门槛金额（满多少可用），0 表示无门槛 */
    private BigDecimal threshold;

    /** 优惠金额（满减） */
    private BigDecimal amount;

    /** 折扣率（折扣券，如 0.88 表示 8.8 折） */
    private BigDecimal discount;

    /** 发放总量，-1 表示不限量 */
    private Integer totalCount;

    /** 已发放数量 */
    private Integer issuedCount;

    /** 每人限领数量 */
    private Integer perLimit;

    /** 有效期天数（领取后 N 天内有效），0 表示用固定起止时间 */
    private Integer validDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /** 状态 0停用 1启用 */
    private Integer status;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
