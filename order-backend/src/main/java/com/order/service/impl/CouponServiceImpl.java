package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.dto.UserCouponVO;
import com.order.entity.Coupon;
import com.order.entity.UserCoupon;
import com.order.mapper.CouponMapper;
import com.order.mapper.UserCouponMapper;
import com.order.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 券状态：0未使用 1已使用 2已过期 */
    private static final int UC_UNUSED = 0;
    private static final int UC_USED = 1;
    private static final int UC_EXPIRED = 2;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Override
    public List<Coupon> listAvailable(String shopId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> list = list(new LambdaQueryWrapper<Coupon>()
                .eq(StringUtils.hasText(shopId), Coupon::getShopId, shopId)
                .eq(Coupon::getStatus, 1)
                .and(w -> w.isNull(Coupon::getEndTime).or().ge(Coupon::getEndTime, now))
                .orderByDesc(Coupon::getAmount)
                .orderByAsc(Coupon::getThreshold));
        // 过滤已领完的（totalCount = -1 表示不限量）
        List<Coupon> result = new ArrayList<>();
        for (Coupon c : list) {
            if (c.getTotalCount() != null && c.getTotalCount() >= 0) {
                int issued = c.getIssuedCount() == null ? 0 : c.getIssuedCount();
                if (issued >= c.getTotalCount()) {
                    continue;
                }
            }
            result.add(c);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(String couponId, String userId, String shopId) {
        if (!StringUtils.hasText(userId)) {
            throw new RuntimeException("用户未登录");
        }
        Coupon coupon = getById(couponId);
        if (coupon == null) {
            throw new RuntimeException("优惠券不存在");
        }
        // 店铺隔离：只能领取当前店铺推出的优惠券
        if (StringUtils.hasText(shopId) && StringUtils.hasText(coupon.getShopId())
                && !shopId.equals(coupon.getShopId())) {
            throw new RuntimeException("该优惠券不属于当前店铺");
        }
        if (coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new RuntimeException("优惠券已下架");
        }
        if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("优惠券已过期");
        }
        if (coupon.getTotalCount() != null && coupon.getTotalCount() >= 0) {
            int issued = coupon.getIssuedCount() == null ? 0 : coupon.getIssuedCount();
            if (issued >= coupon.getTotalCount()) {
                throw new RuntimeException("优惠券已被领完");
            }
        }

        // 每人限领
        int perLimit = coupon.getPerLimit() == null ? 1 : coupon.getPerLimit();
        long received = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (received >= perLimit) {
            throw new RuntimeException("该优惠券每人限领 " + perLimit + " 张");
        }

        // 计算有效期
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end;
        if (coupon.getValidDays() != null && coupon.getValidDays() > 0) {
            end = start.plusDays(coupon.getValidDays());
        } else {
            end = coupon.getEndTime();
        }

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        // 用户券归属券模板所属店铺（店铺隔离）
        uc.setShopId(StringUtils.hasText(coupon.getShopId()) ? coupon.getShopId() : shopId);
        uc.setCouponId(couponId);
        uc.setCouponName(coupon.getName());
        uc.setType(coupon.getType());
        uc.setThreshold(coupon.getThreshold());
        uc.setAmount(coupon.getAmount());
        uc.setDiscount(coupon.getDiscount());
        uc.setStatus(UC_UNUSED);
        uc.setStartTime(start);
        uc.setEndTime(end);
        userCouponMapper.insert(uc);

        // 更新发放数量
        coupon.setIssuedCount((coupon.getIssuedCount() == null ? 0 : coupon.getIssuedCount()) + 1);
        updateById(coupon);
    }

    @Override
    public List<UserCouponVO> listMyCoupons(String userId, Integer status, String shopId) {
        if (!StringUtils.hasText(userId)) {
            return new ArrayList<>();
        }
        List<UserCoupon> list = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(StringUtils.hasText(shopId), UserCoupon::getShopId, shopId)
                .orderByDesc(UserCoupon::getCreateTime));

        List<UserCouponVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (UserCoupon uc : list) {
            // 自动标记过期
            int realStatus = uc.getStatus() == null ? UC_UNUSED : uc.getStatus();
            if (realStatus == UC_UNUSED && uc.getEndTime() != null && uc.getEndTime().isBefore(now)) {
                realStatus = UC_EXPIRED;
                uc.setStatus(UC_EXPIRED);
                userCouponMapper.updateById(uc);
            }
            if (status != null && !status.equals(realStatus)) {
                continue;
            }
            result.add(toVO(uc, realStatus));
        }
        return result;
    }

    @Override
    public Map<String, Long> countMyCoupons(String userId, String shopId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("unused", 0L);
        counts.put("used", 0L);
        counts.put("expired", 0L);
        if (!StringUtils.hasText(userId)) {
            return counts;
        }
        List<UserCouponVO> all = listMyCoupons(userId, null, shopId);
        for (UserCouponVO vo : all) {
            if (vo.getStatus() != null && vo.getStatus() == UC_UNUSED) {
                counts.put("unused", counts.get("unused") + 1);
            } else if (vo.getStatus() != null && vo.getStatus() == UC_USED) {
                counts.put("used", counts.get("used") + 1);
            } else if (vo.getStatus() != null && vo.getStatus() == UC_EXPIRED) {
                counts.put("expired", counts.get("expired") + 1);
            }
        }
        return counts;
    }

    @Override
    public UserCouponVO findBestCoupon(String userId, BigDecimal amount, String shopId) {
        if (!StringUtils.hasText(userId) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        List<UserCouponVO> unused = listMyCoupons(userId, UC_UNUSED, shopId);
        return unused.stream()
                .filter(vo -> calcDiscount(vo, amount).compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(vo -> calcDiscount(vo, amount)))
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCoupon(String userCouponId, String orderId, String userId, BigDecimal amount, String shopId) {
        if (!StringUtils.hasText(userCouponId)) {
            return BigDecimal.ZERO;
        }
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null) {
            throw new RuntimeException("优惠券不存在");
        }
        if (!uc.getUserId().equals(userId)) {
            throw new RuntimeException("无权使用该优惠券");
        }
        // 店铺隔离：券必须属于下单当前店铺才能核销
        if (StringUtils.hasText(shopId) && StringUtils.hasText(uc.getShopId())
                && !shopId.equals(uc.getShopId())) {
            throw new RuntimeException("无权使用该优惠券");
        }
        if (uc.getStatus() != null && uc.getStatus() != UC_UNUSED) {
            throw new RuntimeException("优惠券已使用或已过期");
        }
        LocalDateTime now = LocalDateTime.now();
        if (uc.getEndTime() != null && uc.getEndTime().isBefore(now)) {
            throw new RuntimeException("优惠券已过期");
        }
        if (uc.getStartTime() != null && uc.getStartTime().isAfter(now)) {
            throw new RuntimeException("优惠券尚未生效");
        }

        BigDecimal discount = calcDiscount(toVO(uc, UC_UNUSED), amount);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("订单金额不满足优惠券使用门槛");
        }

        uc.setStatus(UC_USED);
        uc.setUseTime(now);
        uc.setOrderId(orderId);
        userCouponMapper.updateById(uc);
        return discount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundCoupon(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return;
        }
        List<UserCoupon> list = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getOrderId, orderId)
                .eq(UserCoupon::getStatus, UC_USED));
        for (UserCoupon uc : list) {
            // 未过期的退回为未使用，已过期的标记过期
            boolean expired = uc.getEndTime() != null && uc.getEndTime().isBefore(LocalDateTime.now());
            uc.setStatus(expired ? UC_EXPIRED : UC_UNUSED);
            uc.setOrderId(null);
            uc.setUseTime(null);
            userCouponMapper.updateById(uc);
        }
    }

    // ==================== 内部方法 ====================

    /** 计算优惠金额 */
    private BigDecimal calcDiscount(UserCouponVO vo, BigDecimal amount) {
        if (vo.getThreshold() != null && amount.compareTo(vo.getThreshold()) < 0) {
            return BigDecimal.ZERO;
        }
        if (vo.getType() != null && vo.getType() == 2 && vo.getDiscount() != null) {
            // 折扣券：优惠 = 金额 * (1 - 折扣)
            BigDecimal off = amount.multiply(BigDecimal.ONE.subtract(vo.getDiscount()));
            return off.setScale(2, RoundingMode.HALF_UP);
        }
        if (vo.getAmount() != null) {
            // 满减券，优惠不超过订单金额
            return vo.getAmount().min(amount);
        }
        return BigDecimal.ZERO;
    }

    private UserCouponVO toVO(UserCoupon uc, int status) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(uc.getId());
        vo.setCouponId(uc.getCouponId());
        vo.setName(uc.getCouponName());
        vo.setType(uc.getType());
        vo.setThreshold(uc.getThreshold());
        vo.setAmount(uc.getAmount());
        vo.setDiscount(uc.getDiscount());
        vo.setStatus(status);
        vo.setStatusText(statusText(status));
        vo.setStartTime(uc.getStartTime() == null ? null : uc.getStartTime().format(DATE_TIME));
        vo.setEndTime(uc.getEndTime() == null ? null : uc.getEndTime().format(DATE_TIME));
        vo.setLabel(buildLabel(uc));
        return vo;
    }

    private String statusText(int status) {
        switch (status) {
            case UC_UNUSED:
                return "未使用";
            case UC_USED:
                return "已使用";
            case UC_EXPIRED:
                return "已过期";
            default:
                return "未知";
        }
    }

    private String buildLabel(UserCoupon uc) {
        if (uc.getType() != null && uc.getType() == 2 && uc.getDiscount() != null) {
            BigDecimal zhe = uc.getDiscount().multiply(BigDecimal.TEN);
            String zheStr = zhe.stripTrailingZeros().toPlainString();
            if (uc.getThreshold() != null && uc.getThreshold().compareTo(BigDecimal.ZERO) > 0) {
                return "满 " + uc.getThreshold().stripTrailingZeros().toPlainString()
                        + " 元享 " + zheStr + " 折";
            }
            return zheStr + " 折";
        }
        if (uc.getThreshold() != null && uc.getThreshold().compareTo(BigDecimal.ZERO) > 0) {
            return "满 " + uc.getThreshold().stripTrailingZeros().toPlainString()
                    + " 减 " + uc.getAmount().stripTrailingZeros().toPlainString();
        }
        return "立减 " + uc.getAmount().stripTrailingZeros().toPlainString() + " 元";
    }
}
