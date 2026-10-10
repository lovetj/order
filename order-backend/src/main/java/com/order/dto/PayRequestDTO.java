package com.order.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 发起支付请求 DTO
 */
@Data
public class PayRequestDTO {

    /** 订单ID(必传) */
    @NotNull(message = "订单ID不能为空")
    private String orderId;

    /** 支付方式: 1微信 2支付宝 */
    @NotNull(message = "支付方式不能为空")
    private Integer payType;

    /**
     * 客户端平台:
     * mp    - 微信小程序 (JSAPI支付, 需 openid)
     * h5    - 普通H5 (H5支付/支付宝手机网站支付)
     * app   - 原生APP
     */
    private String platform;

    /** 微信 openid */
    private String openid;

    /** 来源回跳URL */
    private String returnUrl;
}
