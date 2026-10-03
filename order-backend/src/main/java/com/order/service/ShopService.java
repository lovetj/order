package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.dto.DashboardVO;
import com.order.entity.Shop;

public interface ShopService extends IService<Shop> {

    /**
     * 获取指定店铺信息（顾客端首页 / 店家端我的）
     *
     * @param shopId 店铺ID
     */
    Shop getShop(String shopId);

    /**
     * 店家端：获取本店信息（不会返回假数据兜底，店铺不存在直接报错）
     *
     * @param shopId 登录态解析出的店铺ID
     */
    Shop getShopForMerchant(String shopId);

    /**
     * 店家端：更新本店信息（只更新允许修改的字段，避免误清空其它字段）
     *
     * @param shopId 登录态解析出的店铺ID
     * @param form   待更新内容
     */
    void updateShopInfo(String shopId, Shop form);

    /**
     * 切换指定店铺营业状态
     */
    void toggleBusinessStatus(String shopId);

    /**
     * 管理后台看板数据（仅统计本店）
     */
    DashboardVO getDashboard(String shopId);
}
