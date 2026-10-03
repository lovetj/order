package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 顾客用户表（微信小程序用户）
 */
@Data
@TableName("user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String username;

    private String password;

    private String phone;

    /** 微信小程序 openid */
    private String openid;

    private String unionid;

    /** 微信会话密钥 session_key（仅服务端保存） */
    private String sessionKey;

    private String avatar;

    private String nickname;

    /** 会员等级：普通会员 / 白银会员 / 黄金会员 / 钻石会员 */
    private String memberLevel;

    /** 积分余额 */
    private Integer points;

    /** 累计消费金额 */
    private java.math.BigDecimal totalConsume;

    /** 累计订单数 */
    private Integer orderCount;

    /** 状态 0禁用 1正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
