package com.order.controller;

import com.order.common.Result;
import com.order.dto.PayRequestDTO;
import com.order.dto.PayResponseDTO;
import com.order.entity.Admin;
import com.order.service.AdminService;
import com.order.service.PaymentService;
import com.order.util.AuthUtil;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    /**
     * 发起线上支付（创建流水 + 返回调起参数）
     */
    @PostMapping("/create")
    public Result<PayResponseDTO> create(
            @Validated @RequestBody PayRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "userId", required = false) String headerUserId,
            HttpServletRequest httpRequest) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        String clientIp = getClientIp(httpRequest);
        PayResponseDTO resp = paymentService.createPayment(request, userId, clientIp);
        if (!resp.isSuccess()) {
            return Result.error(resp.getMessage());
        }
        return Result.success(resp);
    }

    /**
     * Mock 模式模拟支付成功
     */
    @PostMapping("/mock-success")
    public Result<PayResponseDTO> mockSuccess(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "userId", required = false) String headerUserId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        String paymentNo = body.get("paymentNo");
        if (paymentNo == null || paymentNo.isEmpty()) {
            return Result.error("paymentNo 不能为空");
        }
        PayResponseDTO resp = paymentService.mockPaySuccess(paymentNo, userId);
        if (!resp.isSuccess()) {
            return Result.error(resp.getMessage());
        }
        return Result.success(resp);
    }

    /**
     * 查询支付流水状态（前端轮询）
     */
    @GetMapping("/status")
    public Result<PayResponseDTO> status(
            @RequestParam String paymentNo,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "userId", required = false) String headerUserId) {
        String userId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        PayResponseDTO resp = paymentService.queryPaymentStatus(paymentNo, userId);
        if (!resp.isSuccess()) {
            return Result.error(resp.getMessage());
        }
        return Result.success(resp);
    }

    /**
     * 微信异步回调
     */
    @PostMapping("/wechat/notify")
    public String wechatNotify(
            @RequestBody String body,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serialNo) {
        boolean ok = paymentService.handleWechatNotify(body, signature, timestamp, nonce, serialNo);
        if (ok) {
            return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
        }
        return "{\"code\":\"FAIL\",\"message\":\"处理失败\"}";
    }

    /**
     * 店家端确认线下收款
     */
    @PutMapping("/merchant/confirm-offline/{orderId}")
    public Result<Void> confirmOfflinePay(
            @PathVariable String orderId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "userId", required = false) String headerUserId) {
        String adminId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (adminId == null || !AuthUtil.isMerchant(jwtUtil, authorization)) {
            return Result.error(401, "请先以店家身份登录");
        }
        String shopId = ShopContext.resolveMerchantShopId(jwtUtil, authorization);
        if (!StringUtils.hasText(shopId)) {
            Admin admin = adminService.getById(adminId);
            shopId = admin != null ? admin.getShopId() : null;
        }
        paymentService.confirmOfflinePay(orderId, shopId, adminId);
        return Result.success();
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isEmpty() && !"unknown".equalsIgnoreCase(v)) {
                int comma = v.indexOf(',');
                return comma > 0 ? v.substring(0, comma).trim() : v.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
