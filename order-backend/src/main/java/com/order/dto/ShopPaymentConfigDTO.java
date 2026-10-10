package com.order.dto;

import lombok.Data;

/**
 * 店铺支付管理配置 DTO
 */
@Data
public class ShopPaymentConfigDTO {

    /** 所属店铺ID */
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

    /** 微信支付商户号 */
    private String wxMchId;

    /** 微信支付AppId */
    private String wxAppId;

    /** 支付宝应用AppId */
    private String alipayAppId;
}
