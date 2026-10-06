package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.common.PageResult;
import com.order.dto.OrderCreateDTO;
import com.order.dto.OrderVO;
import com.order.entity.Order;

import java.util.Map;

public interface OrderService extends IService<Order> {

    /**
     * 顾客下单（扫码点餐）
     *
     * @param shopId 下单所属店铺（来自扫码桌位）
     * @return 新订单VO（含 id / orderNo / amount 等，便于前端跳转订单页）
     */
    OrderVO createOrder(OrderCreateDTO dto, String userId, String shopId);

    /**
     * 顾客端订单分页列表（跨店铺，按用户维度）
     *
     * @param status 前端状态字符串 pending / cooking / done / canceled，null 或 all 表示全部
     */
    PageResult<OrderVO> pageForCustomer(Integer pageNum, Integer pageSize, String status, String userId, String shopId);

    /**
     * 管理端订单分页列表（仅本店订单）
     */
    PageResult<OrderVO> pageForMerchant(String shopId, Integer pageNum, Integer pageSize, String status);

    /**
     * 订单详情
     */
    OrderVO getDetail(String id, String shopId);

    /**
     * 各状态订单数量（顾客端"我的"入口）
     * 返回: { pending, cooking, done, canceled, all }
     */
    Map<String, Long> countByUser(String userId, String shopId);

    /**
     * 各状态订单数量（管理端 Tab 角标，仅本店）
     */
    Map<String, Long> countForMerchant(String shopId);

    /**
     * 店家接单：待接单 -> 制作中（校验归属店铺）
     */
    void accept(String id, String shopId);

    /**
     * 店家出餐：制作中 -> 已完成（校验归属店铺）
     */
    void finish(String id, String shopId);

    /**
     * 店家拒单：-> 已取消（校验归属店铺）
     */
    void reject(String id, String reason, String shopId);

    /**
     * 顾客取消订单
     */
    void cancel(String id, String userId, String reason, String shopId);
}
