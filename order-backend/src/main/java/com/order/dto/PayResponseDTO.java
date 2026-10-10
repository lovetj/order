package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付响应 DTO
 */
@Data
public class PayResponseDTO {

    /** 是否成功创建支付单/发起支付 */
    private boolean success;

    /** 提示或错误信息 */
    private String message;

    /** 本系统支付流水号 */
    private String paymentNo;

    /** 关联订单号 */
    private String orderNo;

    /** 支付金额 */
    private BigDecimal payAmount;

    /** 支付方式 1微信 2支付宝 */
    private Integer payType;

    /**
     * 前端拉起支付需要的参数 (Mock或真实微信/支付宝参数)
     */
    private Map<String, Object> payParams;
}
