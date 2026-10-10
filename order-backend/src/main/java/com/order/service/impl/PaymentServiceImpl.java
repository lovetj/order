package com.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.config.PayConfig;
import com.order.dto.PayRequestDTO;
import com.order.dto.PayResponseDTO;
import com.order.entity.Order;
import com.order.entity.Payment;
import com.order.entity.ShopPaymentConfig;
import com.order.mapper.OrderMapper;
import com.order.mapper.PaymentMapper;
import com.order.service.DiningTableService;
import com.order.service.PaymentService;
import com.order.service.ShopPaymentConfigService;
import com.order.util.WechatPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

    private final OrderMapper orderMapper;
    private final PayConfig payConfig;
    private final ShopPaymentConfigService shopPaymentConfigService;
    private final DiningTableService diningTableService;

    /** 订单状态：0待接单 1制作中 2已完成 3已取消 */
    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_COOKING = 1;

    /** 支付状态：0待支付 1已支付成功 2支付失败 3已关闭 */
    private static final int PAY_STATUS_PENDING = 0;
    private static final int PAY_STATUS_SUCCESS = 1;
    private static final int PAY_STATUS_FAIL = 2;

    /** 支付方式 */
    private static final int PAY_TYPE_WECHAT = 1;
    private static final int PAY_TYPE_ALIPAY = 2;

    /** 按订单ID粒度的锁 */
    private final Map<String, Object> orderLocks = new ConcurrentHashMap<>();

    private Object lockForOrder(String orderId) {
        return orderLocks.computeIfAbsent(orderId, k -> new Object());
    }

    private String generatePaymentNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = (int) ((Math.random() * 9 + 1) * 1000);
        return "P" + ts + rand;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResponseDTO createPayment(PayRequestDTO request, String userId, String clientIp) {
        PayResponseDTO resp = new PayResponseDTO();

        // 1. 校验订单
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            resp.setSuccess(false);
            resp.setMessage("订单不存在");
            return resp;
        }
        if (!order.getUserId().equals(userId)) {
            resp.setSuccess(false);
            resp.setMessage("无权操作该订单");
            return resp;
        }
        if (order.getStatus() != ORDER_STATUS_PENDING && order.getStatus() != ORDER_STATUS_COOKING) {
            resp.setSuccess(false);
            resp.setMessage("订单当前状态不可发起线上支付");
            return resp;
        }

        // 2. 校验店铺线上支付开关
        ShopPaymentConfig shopConfig = shopPaymentConfigService.getByShopId(order.getShopId());
        if (request.getPayType() == PAY_TYPE_WECHAT && shopConfig != null && shopConfig.getOnlineWxPayEnabled() == 0) {
            resp.setSuccess(false);
            resp.setMessage("商家未开启线上微信支付");
            return resp;
        }
        if (request.getPayType() == PAY_TYPE_ALIPAY && shopConfig != null && shopConfig.getOnlineAlipayPayEnabled() == 0) {
            resp.setSuccess(false);
            resp.setMessage("商家未开启线上支付宝支付");
            return resp;
        }

        // 3. 查重防并发
        String paymentNo = generatePaymentNo();
        Payment payment;
        synchronized (lockForOrder(order.getId())) {
            Payment exist = getOne(new LambdaQueryWrapper<Payment>()
                    .eq(Payment::getOrderId, order.getId())
                    .eq(Payment::getPayStatus, PAY_STATUS_PENDING)
                    .last("LIMIT 1"));

            if (exist != null) {
                if (request.getPayType() != null && !request.getPayType().equals(exist.getPayType())) {
                    exist.setPayType(request.getPayType());
                }
                exist.setPaymentNo(generatePaymentNo());
                updateById(exist);
                payment = exist;
            } else {
                payment = new Payment();
                payment.setPaymentNo(paymentNo);
                payment.setShopId(order.getShopId());
                payment.setOrderId(order.getId());
                payment.setOrderNo(order.getOrderNo());
                payment.setUserId(userId);
                payment.setPayAmount(order.getPayAmount());
                payment.setPayType(request.getPayType());
                payment.setPayStatus(PAY_STATUS_PENDING);
                payment.setRequestParam(JSON.toJSONString(request));
                payment.setClientIp(clientIp);
                payment.setExpireTime(LocalDateTime.now().plusMinutes(30));
                save(payment);
            }
        }

        // 回写订单 paymentNo
        order.setPaymentNo(payment.getPaymentNo());
        order.setPayType(request.getPayType());
        order.setPayMode(1); // 线上
        orderMapper.updateById(order);

        boolean isMock = payConfig.getMock().isEnabled();
        Map<String, Object> payParams = new HashMap<>();

        if (isMock) {
            payParams.put("mock", true);
            payParams.put("paymentNo", payment.getPaymentNo());
            payParams.put("payType", request.getPayType());
            payParams.put("tips", "开发测试Mock模式：请点击模拟支付完成");
        } else {
            // 真实支付分支
            if (request.getPayType() == PAY_TYPE_WECHAT) {
                payParams.put("mock", false);
                payParams.put("channel", "wechat");
                payParams.put("appId", payConfig.getWechat().getAppId());
                payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
                payParams.put("nonceStr", UUID.randomUUID().toString().replace("-", ""));
                payParams.put("package", "prepay_id=mock_prepay_id_" + payment.getPaymentNo());
                payParams.put("signType", "RSA");
                payParams.put("paySign", "mock_sign");
            } else {
                payParams.put("mock", false);
                payParams.put("channel", "alipay");
                payParams.put("serverUrl", payConfig.getAlipay().getServerUrl());
                payParams.put("appId", payConfig.getAlipay().getAppId());
            }
        }

        resp.setSuccess(true);
        resp.setPaymentNo(payment.getPaymentNo());
        resp.setOrderNo(order.getOrderNo());
        resp.setPayAmount(order.getPayAmount());
        resp.setPayType(request.getPayType());
        resp.setPayParams(payParams);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResponseDTO mockPaySuccess(String paymentNo, String userId) {
        PayResponseDTO resp = new PayResponseDTO();
        if (!payConfig.getMock().isEnabled()) {
            resp.setSuccess(false);
            resp.setMessage("当前不是 Mock 模式");
            return resp;
        }

        Payment payment = getOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo)
                .eq(Payment::getUserId, userId));

        if (payment == null) {
            resp.setSuccess(false);
            resp.setMessage("支付流水不存在");
            return resp;
        }

        if (PAY_STATUS_SUCCESS == payment.getPayStatus()) {
            resp.setSuccess(true);
            resp.setPaymentNo(paymentNo);
            resp.setMessage("该流水已处理成功");
            return resp;
        }

        Random rand = new Random();
        int successRate = payConfig.getMock().getSuccessRate();
        boolean success = rand.nextInt(100) < successRate;

        if (success) {
            payment.setPayStatus(PAY_STATUS_SUCCESS);
            payment.setPayTime(LocalDateTime.now());
            payment.setTransactionId("MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            updateById(payment);

            Order order = orderMapper.selectById(payment.getOrderId());
            if (order != null) {
                order.setPayStatus(2); // 已支付
                order.setPayTime(LocalDateTime.now());
                order.setPayType(payment.getPayType());
                order.setPayMode(1);
                if (order.getStatus() == ORDER_STATUS_PENDING) {
                    order.setStatus(ORDER_STATUS_COOKING); // 支付成功自动流转至制作中
                }
                orderMapper.updateById(order);

                if (order.getDiningType() != null && order.getDiningType() == 1 && StringUtils.hasText(order.getTableNo())) {
                    diningTableService.refreshTableStatus(order.getShopId(), order.getTableNo());
                }
            }
            log.info("[Mock支付] 成功 paymentNo={} order={}", paymentNo, payment.getOrderNo());
        } else {
            payment.setPayStatus(PAY_STATUS_FAIL);
            payment.setFailReason("模拟支付失败");
            updateById(payment);
            log.warn("[Mock支付] 模拟失败 paymentNo={}", paymentNo);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("payStatus", payment.getPayStatus());
        params.put("paid", PAY_STATUS_SUCCESS == payment.getPayStatus());
        resp.setSuccess(success);
        resp.setMessage(success ? "支付成功" : "模拟支付失败");
        resp.setPaymentNo(paymentNo);
        resp.setOrderNo(payment.getOrderNo());
        resp.setPayAmount(payment.getPayAmount());
        resp.setPayType(payment.getPayType());
        resp.setPayParams(params);
        return resp;
    }

    @Override
    public PayResponseDTO queryPaymentStatus(String paymentNo, String userId) {
        PayResponseDTO resp = new PayResponseDTO();
        Payment payment = getOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo)
                .eq(Payment::getUserId, userId));

        if (payment == null) {
            resp.setSuccess(false);
            resp.setMessage("支付流水不存在");
            return resp;
        }

        resp.setPaymentNo(paymentNo);
        resp.setOrderNo(payment.getOrderNo());
        resp.setPayAmount(payment.getPayAmount());
        resp.setPayType(payment.getPayType());

        Map<String, Object> params = new HashMap<>();
        params.put("payStatus", payment.getPayStatus());
        params.put("paid", PAY_STATUS_SUCCESS == payment.getPayStatus());
        resp.setPayParams(params);
        resp.setSuccess(true);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleWechatNotify(String notifyBody, String signature, String timestamp, String nonce, String serialNo) {
        try {
            if (!StringUtils.hasText(notifyBody)) return false;
            PayConfig.Wechat wx = payConfig.getWechat();
            @SuppressWarnings("unchecked")
            Map<String, Object> rootMap = JSON.parseObject(notifyBody, Map.class);
            if (rootMap == null) return false;

            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) rootMap.get("resource");
            if (resource == null) return false;

            String ciphertext = (String) resource.get("ciphertext");
            String associatedData = (String) resource.get("associated_data");
            String resourceNonce = (String) resource.get("nonce");

            String plainJson = WechatPayUtil.decryptAesGcm(wx.getApiV3Key(), associatedData, resourceNonce, ciphertext);
            Map<String, Object> plainMap = JSON.parseObject(plainJson);
            String outTradeNo = (String) plainMap.get("out_trade_no");
            String transactionId = (String) plainMap.get("transaction_id");
            String tradeState = (String) plainMap.get("trade_state");

            if ("SUCCESS".equalsIgnoreCase(tradeState)) {
                Payment payment = getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, outTradeNo));
                if (payment != null && PAY_STATUS_SUCCESS != payment.getPayStatus()) {
                    payment.setPayStatus(PAY_STATUS_SUCCESS);
                    payment.setTransactionId(transactionId);
                    payment.setPayTime(LocalDateTime.now());
                    payment.setNotifyData(plainJson);
                    updateById(payment);

                    Order order = orderMapper.selectById(payment.getOrderId());
                    if (order != null) {
                        order.setPayStatus(2);
                        order.setPayTime(LocalDateTime.now());
                        order.setPayType(payment.getPayType());
                        order.setPayMode(1);
                        if (order.getStatus() == ORDER_STATUS_PENDING) {
                            order.setStatus(ORDER_STATUS_COOKING);
                        }
                        orderMapper.updateById(order);
                        if (order.getDiningType() != null && order.getDiningType() == 1 && StringUtils.hasText(order.getTableNo())) {
                            diningTableService.refreshTableStatus(order.getShopId(), order.getTableNo());
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("[微信回调] 处理异常", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOfflinePay(String orderId, String shopId, String adminId) {
        if (!StringUtils.hasText(orderId)) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderNo, orderId).last("LIMIT 1"));
        }
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(order.getShopId())) {
            throw new RuntimeException("无权操作其他店铺订单");
        }
        order.setPayStatus(2); // 已支付
        order.setPayMode(2);   // 线下
        order.setOfflinePayTime(LocalDateTime.now());
        order.setOfflinePayOperator(adminId);
        if (order.getPayTime() == null) {
            order.setPayTime(LocalDateTime.now());
        }
        orderMapper.updateById(order);
        log.info("[线下收款] 店家确认收款 orderNo={}, shopId={}, adminId={}", order.getOrderNo(), shopId, adminId);
    }
}
