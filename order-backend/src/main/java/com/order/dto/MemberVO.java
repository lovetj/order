package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员信息 VO —— 顾客端「我的」页
 */
@Data
public class MemberVO {
    private String userId;
    private String nickName;
    private String avatar;
    /** 会员等级 */
    private String memberLevel;
    /** 等级序号，0普通 1白银 2黄金 3钻石 */
    private Integer levelIndex;
    /** 积分 */
    private Integer points;
    /** 累计消费 */
    private BigDecimal totalConsume;
    /** 累计订单数 */
    private Integer orderCount;
    /** 升级到下一等级还差多少消费 */
    private BigDecimal nextLevelNeed;
    /** 下一等级名称 */
    private String nextLevelName;
    /** 当前等级进度百分比（0-100） */
    private Integer progress;
    /** 我的优惠券数量 */
    private Long couponCount;
}
