package com.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺支付管理配置表（多店铺隔离）
 */
@Data
@TableName("shop_payment_config")
public class ShopPaymentConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属店铺ID（唯一） */
    private String shopId;

    /** 线下支付总开关 0关闭 1开启 */
    private Integer offlinePayEnabled;

    /** 线下支付-微信收款码图片URL */
    private String offlineWxQrcode;

    /** 线下支付-支付宝收款码图片URL */
    private String offlineAlipayQrcode;

    /** 线下支付-其他收款码/聚合码图片URL */
    private String offlineOtherQrcode;

    /** 线上微信支付开关 0关闭 1开启 */
    private Integer onlineWxPayEnabled;

    /** 线上支付宝支付开关 0关闭 1开启 */
    private Integer onlineAlipayPayEnabled;

    /** 微信支付商户号(可选商户独立配置) */
    private String wxMchId;

    /** 微信支付AppId(可选商户独立配置) */
    private String wxAppId;

    /** 支付宝应用AppId(可选商户独立配置) */
    private String alipayAppId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
