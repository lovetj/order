package com.order.service;

import com.order.dto.MemberVO;

public interface MemberService {

    /**
     * 获取会员信息（含等级、积分、成长进度）
     */
    MemberVO getMemberInfo(String userId);

    /**
     * 订单完成后累加积分与消费额、刷新会员等级
     *
     * @param userId 用户ID
     * @param amount 订单实付金额
     */
    void addOrderReward(String userId, java.math.BigDecimal amount);

    /**
     * 扣减积分（积分抵扣时使用）
     */
    void deductPoints(String userId, Integer points);
}
