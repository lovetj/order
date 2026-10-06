package com.order.service;

import com.order.dto.MemberVO;

public interface MemberService {

    /**
     * 获取会员信息（含等级、积分、成长进度）
     *
     * <p>积分按商家隔离：传入 shopId 时返回该店积分余额；不传时回退全局 user.points。
     */
    MemberVO getMemberInfo(String userId);

    MemberVO getMemberInfo(String userId, String shopId);

    /**
     * 订单完成后累加积分与消费额、刷新会员等级
     *
     * @param userId 用户ID
     * @param shopId 店铺ID（积分累加到该店账本；为空时回退全局 user.points）
     * @param amount 订单实付金额
     */
    void addOrderReward(String userId, String shopId, java.math.BigDecimal amount);

    void addOrderReward(String userId, java.math.BigDecimal amount);

    /**
     * 扣减积分（积分抵扣时使用），按店铺隔离
     *
     * @param userId 用户ID
     * @param shopId 店铺ID（从该店账本扣减；为空时回退全局 user.points）
     * @param points 扣减积分
     */
    void deductPoints(String userId, String shopId, Integer points);

    void deductPoints(String userId, Integer points);
}