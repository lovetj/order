package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.entity.PointsExchange;
import com.order.entity.PointsGoods;

import java.util.List;

public interface PointsService extends IService<PointsGoods> {

    /**
     * 积分商城可兑换商品列表（上架中），按店铺隔离
     */
    List<PointsGoods> listAvailable(String shopId);

    /**
     * 店家端积分商品列表（含下架，按店铺隔离），出参图片拼完整地址
     */
    List<PointsGoods> listForAdmin(String shopId);

    /**
     * 积分兑换
     *
     * @param shopId 商品所属店铺（从商品读取，实现店铺隔离）
     * @return 兑换记录（含核销码）
     */
    PointsExchange exchange(String goodsId, Integer quantity, String userId, String shopId);

    /**
     * 我的兑换记录（可按店铺过滤）
     */
    List<PointsExchange> listMyExchanges(String userId, String shopId);

    /**
     * 店家端核销（校验兑换记录归属店铺）
     */
    void verify(String verifyCode, String shopId);
}
