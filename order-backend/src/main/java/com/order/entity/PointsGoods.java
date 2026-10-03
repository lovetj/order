package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分商城商品表
 */
@Data
@TableName("points_goods")
public class PointsGoods implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属店铺ID（多店铺隔离） */
    private String shopId;

    private String name;

    private String description;

    private String image;

    /** 兑换所需积分 */
    private Integer points;

    /** 库存，-1 表示不限量 */
    private Integer stock;

    /** 已兑换数量 */
    private Integer exchangedCount;

    /** 每人限兑数量 */
    private Integer perLimit;

    /** 排序 */
    private Integer sort;

    /** 状态 0下架 1上架 */
    private Integer status;

    private Integer isDel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
