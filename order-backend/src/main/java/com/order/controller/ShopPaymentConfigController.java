package com.order.controller;

import com.order.common.Result;
import com.order.config.FileConfigProperties;
import com.order.dto.ShopPaymentConfigDTO;
import com.order.entity.Admin;
import com.order.entity.ShopPaymentConfig;
import com.order.service.AdminService;
import com.order.service.ShopPaymentConfigService;
import com.order.util.AuthUtil;
import com.order.util.FileUrlUtil;
import com.order.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class ShopPaymentConfigController {

    private final ShopPaymentConfigService shopPaymentConfigService;
    private final AdminService adminService;
    private final FileConfigProperties fileConfigProperties;
    private final JwtUtil jwtUtil;

    /**
     * 店家端：获取本店支付设置
     */
    @GetMapping("/merchant/config")
    public Result<ShopPaymentConfig> getMerchantConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "userId", required = false) String headerUserId) {
        String adminId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (adminId == null || !AuthUtil.isMerchant(jwtUtil, authorization)) {
            return Result.error(401, "请先以店家身份登录");
        }
        Admin admin = adminService.getById(adminId);
        if (admin == null || admin.getShopId() == null) {
            return Result.error(401, "店家未绑定有效店铺");
        }
        ShopPaymentConfig config = shopPaymentConfigService.getByShopId(admin.getShopId());
        if (config != null) {
            String baseServer = fileConfigProperties.getBaseServer();
            config.setOfflineWxQrcode(FileUrlUtil.toAbsolute(config.getOfflineWxQrcode(), baseServer));
            config.setOfflineAlipayQrcode(FileUrlUtil.toAbsolute(config.getOfflineAlipayQrcode(), baseServer));
            config.setOfflineOtherQrcode(FileUrlUtil.toAbsolute(config.getOfflineOtherQrcode(), baseServer));
        }
        return Result.success(config);
    }

    /**
     * 店家端：更新本店支付设置
     */
    @PutMapping("/merchant/config")
    public Result<Void> updateMerchantConfig(
            @RequestBody ShopPaymentConfigDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "userId", required = false) String headerUserId) {
        String adminId = AuthUtil.resolveUserId(jwtUtil, authorization, headerUserId);
        if (adminId == null || !AuthUtil.isMerchant(jwtUtil, authorization)) {
            return Result.error(401, "请先以店家身份登录");
        }
        Admin admin = adminService.getById(adminId);
        if (admin == null || admin.getShopId() == null) {
            return Result.error(401, "店家未绑定有效店铺");
        }
        if (dto.getOfflineWxQrcode() != null) {
            dto.setOfflineWxQrcode(FileUrlUtil.toRelative(dto.getOfflineWxQrcode()));
        }
        if (dto.getOfflineAlipayQrcode() != null) {
            dto.setOfflineAlipayQrcode(FileUrlUtil.toRelative(dto.getOfflineAlipayQrcode()));
        }
        if (dto.getOfflineOtherQrcode() != null) {
            dto.setOfflineOtherQrcode(FileUrlUtil.toRelative(dto.getOfflineOtherQrcode()));
        }
        shopPaymentConfigService.saveOrUpdateConfig(admin.getShopId(), dto);
        return Result.success();
    }

    /**
     * 顾客端/公开：查询指定店铺的支付开关与线下收款码
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getShopPaymentConfig(@RequestParam(required = false) String shopId) {
        if (shopId == null || shopId.isEmpty()) {
            shopId = "1";
        }
        ShopPaymentConfig config = shopPaymentConfigService.getByShopId(shopId);
        Map<String, Object> map = new HashMap<>();
        String baseServer = fileConfigProperties.getBaseServer();
        if (config != null) {
            map.put("offlinePayEnabled", config.getOfflinePayEnabled() == null || config.getOfflinePayEnabled() == 1);
            map.put("offlineWxQrcode", FileUrlUtil.toAbsolute(config.getOfflineWxQrcode(), baseServer));
            map.put("offlineAlipayQrcode", FileUrlUtil.toAbsolute(config.getOfflineAlipayQrcode(), baseServer));
            map.put("offlineOtherQrcode", FileUrlUtil.toAbsolute(config.getOfflineOtherQrcode(), baseServer));
            map.put("onlineWxPayEnabled", config.getOnlineWxPayEnabled() != null && config.getOnlineWxPayEnabled() == 1);
            map.put("onlineAlipayPayEnabled", config.getOnlineAlipayPayEnabled() != null && config.getOnlineAlipayPayEnabled() == 1);
        } else {
            map.put("offlinePayEnabled", true);
            map.put("onlineWxPayEnabled", false);
            map.put("onlineAlipayPayEnabled", false);
        }
        return Result.success(map);
    }
}
