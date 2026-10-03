package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店表
 */
@Data
@TableName("shop")
public class Shop implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    /** 店家真实姓名（原 admin.real_name 迁移而来） */
    private String realName;

    private String logo;

    /** 店铺图片（多张，逗号分隔的相对路径） */
    private String images;

    private String slogan;

    private String notice;

    private String address;

    /** 店铺定位纬度（微信地图选点） */
    private java.math.BigDecimal latitude;

    /** 店铺定位经度（微信地图选点） */
    private java.math.BigDecimal longitude;

    private String phone;

    /** 评分 */
    private java.math.BigDecimal score;

    /** 月销量 */
    private Integer monthSales;

    /** 营业状态 0休息中 1营业中 */
    private Integer businessStatus;

    /** 状态 0禁用 1启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
