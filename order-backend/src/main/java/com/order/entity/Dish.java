package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品表
 */
@Data
@TableName("dish")
public class Dish implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属店铺ID（多店铺隔离） */
    private String shopId;

    private String categoryId;

    private String name;

    /** 菜品描述，对应前端 desc 字段 */
    private String description;

    private String image;

    private BigDecimal price;

    private Integer stock;

    private Integer sales;

    /** 是否热销推荐 0否 1是 */
    private Integer isHot;

    /** 状态 0下架 1上架（前端 status: boolean） */
    private Integer status;

    private Integer sort;

    private Integer isDel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String categoryName;
}
