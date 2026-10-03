package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.dto.UserCouponVO;
import com.order.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService extends IService<Coupon> {

    /**
     * 可领取的优惠券列表（启用中、未领完、未过期），按店铺隔离
     */
    List<Coupon> listAvailable(String shopId);

    /**
     * 领取优惠券
     *
     * @param shopId 券所属店铺（从券模板读取并写入用户券，实现店铺隔离）
     */
    void receiveCoupon(String couponId, String userId, String shopId);

    /**
     * 我的优惠券（可按店铺过滤）
     *
     * @param status 0未使用 1已使用 2已过期，null 全部
     * @param shopId 非空时仅返回该店铺券
     */
    List<UserCouponVO> listMyCoupons(String userId, Integer status, String shopId);

    /**
     * 我的优惠券数量统计（可按店铺过滤）
     * 返回: { unused, used, expired }
     */
    java.util.Map<String, Long> countMyCoupons(String userId, String shopId);

    /**
     * 计算某金额下可用的最优优惠券（限定店铺）
     *
     * @param amount 订单金额
     * @return 可用的优惠券 VO（优惠力度最大的一张），无则返回 null
     */
    UserCouponVO findBestCoupon(String userId, BigDecimal amount, String shopId);

    /**
     * 结算时计算优惠金额并标记券已使用
     *
     * @param userCouponId 用户券ID，可为空
     * @param orderId      订单ID
     * @param amount       订单原始金额
     * @return 实际优惠金额
     */
    BigDecimal useCoupon(String userCouponId, String orderId, String userId, BigDecimal amount);

    /**
     * 订单取消时退回优惠券
     */
    void refundCoupon(String orderId);
}
