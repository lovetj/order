package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.dto.ShopPaymentConfigDTO;
import com.order.entity.ShopPaymentConfig;
import com.order.mapper.ShopPaymentConfigMapper;
import com.order.service.ShopPaymentConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class ShopPaymentConfigServiceImpl extends ServiceImpl<ShopPaymentConfigMapper, ShopPaymentConfig> implements ShopPaymentConfigService {

    @Override
    public ShopPaymentConfig getByShopId(String shopId) {
        if (!StringUtils.hasText(shopId)) {
            return null;
        }
        ShopPaymentConfig config = getOne(new LambdaQueryWrapper<ShopPaymentConfig>()
                .eq(ShopPaymentConfig::getShopId, shopId)
                .last("LIMIT 1"));

        if (config == null) {
            // 初始化默认配置：线下开启，线上微信/支付宝默认关闭
            config = new ShopPaymentConfig();
            config.setShopId(shopId);
            config.setOfflinePayEnabled(1);
            config.setOnlineWxPayEnabled(0);
            config.setOnlineAlipayPayEnabled(0);
            save(config);
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateConfig(String shopId, ShopPaymentConfigDTO dto) {
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("店铺ID不能为空");
        }
        ShopPaymentConfig config = getOne(new LambdaQueryWrapper<ShopPaymentConfig>()
                .eq(ShopPaymentConfig::getShopId, shopId)
                .last("LIMIT 1"));

        if (config == null) {
            config = new ShopPaymentConfig();
            config.setShopId(shopId);
        }

        config.setOfflinePayEnabled(dto.getOfflinePayEnabled() != null ? dto.getOfflinePayEnabled() : 1);
        config.setOfflineWxQrcode(dto.getOfflineWxQrcode());
        config.setOfflineAlipayQrcode(dto.getOfflineAlipayQrcode());
        config.setOfflineOtherQrcode(dto.getOfflineOtherQrcode());
        config.setOnlineWxPayEnabled(dto.getOnlineWxPayEnabled() != null ? dto.getOnlineWxPayEnabled() : 0);
        config.setOnlineAlipayPayEnabled(dto.getOnlineAlipayPayEnabled() != null ? dto.getOnlineAlipayPayEnabled() : 0);

        if (dto.getWxMchId() != null) config.setWxMchId(dto.getWxMchId());
        if (dto.getWxAppId() != null) config.setWxAppId(dto.getWxAppId());
        if (dto.getAlipayAppId() != null) config.setAlipayAppId(dto.getAlipayAppId());

        saveOrUpdate(config);
        log.info("[支付管理] 更新店铺支付设置 shopId={}, offline={}, wx={}, alipay={}",
                shopId, config.getOfflinePayEnabled(), config.getOnlineWxPayEnabled(), config.getOnlineAlipayPayEnabled());
    }
}
