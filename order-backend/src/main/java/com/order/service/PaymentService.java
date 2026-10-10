package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.dto.PayRequestDTO;
import com.order.dto.PayResponseDTO;
import com.order.entity.Payment;

public interface PaymentService extends IService<Payment> {

    /**
     * 发起支付（创建支付流水并返回前端调起支付参数）
     */
    PayResponseDTO createPayment(PayRequestDTO request, String userId, String clientIp);

    /**
     * Mock 模式模拟支付成功
     */
    PayResponseDTO mockPaySuccess(String paymentNo, String userId);

    /**
     * 查询支付流水状态
     */
    PayResponseDTO queryPaymentStatus(String paymentNo, String userId);

    /**
     * 微信异步回调处理
     */
    boolean handleWechatNotify(String notifyBody, String signature, String timestamp, String nonce, String serialNo);

    /**
     * 店家端确认线下支付收款
     */
    void confirmOfflinePay(String orderId, String shopId, String adminId);
}
