package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细 VO —— 字段与前端 order.items 对齐
 * { name, count, price }
 */
@Data
public class OrderItemVO {
    private String dishId;
    private String name;
    /** 前端使用 count */
    private Integer count;
    private BigDecimal price;
    private BigDecimal amount;
    private String image;
    /** 规格描述，如「微辣, 加蛋」 */
    private String specText;
}
