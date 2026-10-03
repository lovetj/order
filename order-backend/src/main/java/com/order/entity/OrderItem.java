package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细表
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属店铺ID（多店铺隔离） */
    private String shopId;

    private String orderId;

    private String dishId;

    /** 菜品名称（下单时快照） */
    private String dishName;

    /** 菜品图片（下单时快照） */
    private String dishImage;

    /** 单价（下单时快照） */
    private BigDecimal price;

    /** 数量，前端 count */
    private Integer quantity;

    /** 小计 = price * quantity */
    private BigDecimal amount;

    /** 规格描述（下单时快照，如「微辣, 加蛋」） */
    private String specText;

    /** 规格加价合计 */
    private BigDecimal specPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
