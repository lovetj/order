package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店家（商家）账号表
 */
@Data
@TableName("admin")
public class Admin implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String username;

    private String password;

    /**
     * 头像（存相对路径，如 /avatar/20261004/xxx.jpg）
     * 说明：真实姓名已迁移到 shop.real_name，手机号统一使用 shop.phone
     */
    private String avatar;

    /** 关联门店ID */
    private String shopId;

    /** 状态 0禁用 1正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
