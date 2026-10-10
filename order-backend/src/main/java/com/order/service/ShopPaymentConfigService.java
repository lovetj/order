package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.dto.ShopPaymentConfigDTO;
import com.order.entity.ShopPaymentConfig;

public interface ShopPaymentConfigService extends IService<ShopPaymentConfig> {

    /**
     * 根据店铺ID查询配置（若不存在自动初始化默认配置）
     */
    ShopPaymentConfig getByShopId(String shopId);

    /**
     * 保存/更新店铺支付配置（多店铺隔离）
     */
    void saveOrUpdateConfig(String shopId, ShopPaymentConfigDTO dto);
}
