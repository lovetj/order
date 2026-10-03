package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分兑换记录表
 */
@Data
@TableName("points_exchange")
public class PointsExchange implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属店铺ID（多店铺隔离） */
    private String shopId;

    /** 兑换单号 */
    private String exchangeNo;

    private String userId;

    private String goodsId;

    /** 商品名称（快照） */
    private String goodsName;

    /** 商品图片（快照） */
    private String goodsImage;

    /** 消耗积分 */
    private Integer points;

    /** 数量 */
    private Integer quantity;

    /** 状态 0待发放 1已发放 2已核销 */
    private Integer status;

    /** 核销码（到店出示） */
    private String verifyCode;

    private LocalDateTime verifyTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
