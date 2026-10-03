package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品规格/口味选项表
 * 例：辣度（不辣/微辣/中辣/特辣）、温度（常温/加冰）、加料（加蛋+2元）
 */
@Data
@TableName("dish_spec")
public class DishSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属店铺ID（多店铺隔离） */
    private String shopId;

    private String dishId;

    /** 规格分组名，如「辣度」「加料」 */
    private String groupName;

    /** 选项名，如「微辣」「加蛋」 */
    private String name;

    /** 加价（可为 0，加料类通常 >0） */
    private BigDecimal extraPrice;

    /** 是否为默认选中 0否 1是 */
    private Integer isDefault;

    /**
     * 选择类型：
     * 1 单选（如辣度）
     * 2 多选（如加料）
     */
    private Integer selectType;

    /**
     * 是否必选 0否 1是（单选组常设为必选）
     */
    private Integer required;

    private Integer sort;

    /** 状态 0停用 1启用 */
    private Integer status;

    private Integer isDel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
