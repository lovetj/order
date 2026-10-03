 package com.order.service.impl;

import com.order.dto.MemberVO;
import com.order.entity.User;
import com.order.service.CouponService;
import com.order.service.MemberService;
import com.order.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class MemberServiceImpl implements MemberService {

    /** 每消费 1 元获得 1 积分 */
    private static final int POINTS_PER_YUAN = 1;

    /** 会员等级门槛（累计消费） */
    private static final BigDecimal[] LEVEL_THRESHOLDS = {
            BigDecimal.ZERO,
            new BigDecimal("500"),
            new BigDecimal("2000"),
            new BigDecimal("5000")
    };
    private static final String[] LEVEL_NAMES = { "普通会员", "白银会员", "黄金会员", "钻石会员" };

    @Autowired
    private UserService userService;

    @Autowired
    private CouponService couponService;

    @Override
    public MemberVO getMemberInfo(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new RuntimeException("用户未登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        int levelIndex = resolveLevelIndex(user.getTotalConsume());
        MemberVO vo = new MemberVO();
        vo.setUserId(user.getId());
        vo.setNickName(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setMemberLevel(LEVEL_NAMES[levelIndex]);
        vo.setLevelIndex(levelIndex);
        vo.setPoints(user.getPoints() == null ? 0 : user.getPoints());
        vo.setTotalConsume(user.getTotalConsume() == null ? BigDecimal.ZERO : user.getTotalConsume());
        vo.setOrderCount(user.getOrderCount() == null ? 0 : user.getOrderCount());

        // 升级进度
        if (levelIndex >= LEVEL_NAMES.length - 1) {
            vo.setNextLevelName("已是最高等级");
            vo.setNextLevelNeed(BigDecimal.ZERO);
            vo.setProgress(100);
        } else {
            BigDecimal current = LEVEL_THRESHOLDS[levelIndex];
            BigDecimal next = LEVEL_THRESHOLDS[levelIndex + 1];
            BigDecimal consume = vo.getTotalConsume();
            BigDecimal gap = next.subtract(consume);
            vo.setNextLevelName(LEVEL_NAMES[levelIndex + 1]);
            vo.setNextLevelNeed(gap.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : gap);
            BigDecimal range = next.subtract(current);
            int progress = range.compareTo(BigDecimal.ZERO) > 0
                    ? consume.subtract(current).multiply(BigDecimal.valueOf(100))
                            .divide(range, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            vo.setProgress(Math.max(0, Math.min(100, progress)));
        }

        // 优惠券数量（会员中心为顾客维度，统计其全部店铺的券）
        try {
            Map<String, Long> counts = couponService.countMyCoupons(userId, null);
            vo.setCouponCount(counts.getOrDefault("unused", 0L));
        } catch (Exception e) {
            vo.setCouponCount(0L);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOrderReward(String userId, BigDecimal amount) {
        if (!StringUtils.hasText(userId) || amount == null) {
            return;
        }
        User user = userService.getById(userId);
        if (user == null) {
            return;
        }
        // 积分
        int addPoints = amount.multiply(BigDecimal.valueOf(POINTS_PER_YUAN))
                .setScale(0, RoundingMode.DOWN).intValue();
        user.setPoints((user.getPoints() == null ? 0 : user.getPoints()) + addPoints);

        // 累计消费与订单数
        BigDecimal total = user.getTotalConsume() == null ? BigDecimal.ZERO : user.getTotalConsume();
        user.setTotalConsume(total.add(amount));
        user.setOrderCount((user.getOrderCount() == null ? 0 : user.getOrderCount()) + 1);

        // 刷新会员等级
        user.setMemberLevel(LEVEL_NAMES[resolveLevelIndex(user.getTotalConsume())]);
        userService.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(String userId, Integer points) {
        if (!StringUtils.hasText(userId) || points == null || points <= 0) {
            return;
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        int current = user.getPoints() == null ? 0 : user.getPoints();
        if (current < points) {
            throw new RuntimeException("积分不足");
        }
        user.setPoints(current - points);
        userService.updateById(user);
    }

    private int resolveLevelIndex(BigDecimal totalConsume) {
        BigDecimal consume = totalConsume == null ? BigDecimal.ZERO : totalConsume;
        int index = 0;
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (consume.compareTo(LEVEL_THRESHOLDS[i]) >= 0) {
                index = i;
                break;
            }
        }
        return index;
    }
}
