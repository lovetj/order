package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 餐桌表（扫码点餐桌位二维码管理）
 */
@Data
@TableName("dining_table")
public class DiningTable implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String shopId;

    /** 楼号/楼层，如 1楼、2楼、A栋1楼 */
    @TableField("building_no")
    private String buildingNo;

    /** 类型：大厅 / 包房 */
    @TableField("type")
    private String type;

    /** 桌位别名，如 牡丹阁、VIP1、靠窗位 */
    @TableField("alias")
    private String alias;

    /** 桌号，如 A01 */
    private String tableNo;

    /** 容纳人数 */
    private Integer capacity;

    /** 桌位二维码图片 */
    private String qrCode;

    /** 状态 0停用 1启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
