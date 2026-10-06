package com.order.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.config.FileConfigProperties;
import com.order.entity.PointsExchange;
import com.order.entity.PointsGoods;
import com.order.entity.User;
import com.order.mapper.PointsExchangeMapper;
import com.order.mapper.PointsGoodsMapper;
import com.order.service.MemberService;
import com.order.service.PointsService;
import com.order.service.UserService;
import com.order.util.FileUrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PointsServiceImpl extends ServiceImpl<PointsGoodsMapper, PointsGoods> implements PointsService {

    @Autowired
    private PointsExchangeMapper pointsExchangeMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private FileConfigProperties fileConfigProperties;

    @Override
    public List<PointsGoods> listAvailable(String shopId) {
        List<PointsGoods> list = list(new LambdaQueryWrapper<PointsGoods>()
                .eq(StringUtils.hasText(shopId), PointsGoods::getShopId, shopId)
                .eq(PointsGoods::getStatus, 1)
                .eq(PointsGoods::getIsDel, 0)
                .orderByAsc(PointsGoods::getSort));
        String baseServer = fileConfigProperties.getBaseServer();
        list.forEach(g -> g.setImage(FileUrlUtil.toAbsoluteIfImage(g.getImage(), baseServer)));
        return list;
    }

    @Override
    public List<PointsGoods> listForAdmin(String shopId) {
        List<PointsGoods> list = list(new LambdaQueryWrapper<PointsGoods>()
                .eq(PointsGoods::getShopId, shopId)
                .eq(PointsGoods::getIsDel, 0)
                .orderByAsc(PointsGoods::getSort));
        String baseServer = fileConfigProperties.getBaseServer();
        list.forEach(g -> g.setImage(FileUrlUtil.toAbsoluteIfImage(g.getImage(), baseServer)));
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointsExchange exchange(String goodsId, Integer quantity, String userId, String shopId) {
        if (!StringUtils.hasText(userId)) {
            throw new RuntimeException("用户未登录");
        }
        int qty = (quantity == null || quantity <= 0) ? 1 : quantity;

        PointsGoods goods = getById(goodsId);
        if (goods == null || (goods.getIsDel() != null && goods.getIsDel() == 1)) {
            throw new RuntimeException("商品不存在");
        }
        // 店铺隔离：商品必须属于当前店铺
        if (StringUtils.hasText(goods.getShopId()) && StringUtils.hasText(shopId)
                && !shopId.equals(goods.getShopId())) {
            throw new RuntimeException("商品不属于当前店铺");
        }
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }
        if (goods.getStock() != null && goods.getStock() >= 0 && qty > goods.getStock()) {
            throw new RuntimeException("库存不足，仅剩 " + goods.getStock());
        }

        // 每人限兑
        if (goods.getPerLimit() != null && goods.getPerLimit() > 0) {
            long exchanged = pointsExchangeMapper.selectCount(new LambdaQueryWrapper<PointsExchange>()
                    .eq(PointsExchange::getUserId, userId)
                    .eq(PointsExchange::getGoodsId, goodsId));
            if (exchanged + qty > goods.getPerLimit()) {
                throw new RuntimeException("该商品每人限兑 " + goods.getPerLimit() + " 件");
            }
        }

        int totalPoints = goods.getPoints() * qty;
        // 兑换消耗积分归属商品所属店铺（店铺隔离）
        String goodsShopId = StringUtils.hasText(goods.getShopId()) ? goods.getShopId() : shopId;

        // 扣减积分（按店铺账本，内部校验余额）
        memberService.deductPoints(userId, goodsShopId, totalPoints);

        // 生成兑换记录
        PointsExchange exchange = new PointsExchange();
        exchange.setExchangeNo("E" + System.currentTimeMillis() + RandomUtil.randomNumbers(3));
        exchange.setUserId(userId);
        // 兑换记录归属商品所属店铺（店铺隔离）
        exchange.setShopId(goodsShopId);
        exchange.setGoodsId(goodsId);
        exchange.setGoodsName(goods.getName());
        exchange.setGoodsImage(goods.getImage());
        exchange.setPoints(totalPoints);
        exchange.setQuantity(qty);
        exchange.setStatus(0);
        exchange.setVerifyCode(RandomUtil.randomNumbers(8));
        pointsExchangeMapper.insert(exchange);

        // 扣库存、加兑换数
        if (goods.getStock() != null && goods.getStock() >= 0) {
            goods.setStock(goods.getStock() - qty);
        }
        goods.setExchangedCount((goods.getExchangedCount() == null ? 0 : goods.getExchangedCount()) + qty);
        updateById(goods);

        return exchange;
    }

    @Override
    public List<PointsExchange> listMyExchanges(String userId, String shopId) {
        if (!StringUtils.hasText(userId)) {
            return java.util.Collections.emptyList();
        }
        List<PointsExchange> list = pointsExchangeMapper.selectList(new LambdaQueryWrapper<PointsExchange>()
                .eq(PointsExchange::getUserId, userId)
                .eq(StringUtils.hasText(shopId), PointsExchange::getShopId, shopId)
                .orderByDesc(PointsExchange::getCreateTime));
        String baseServer = fileConfigProperties.getBaseServer();
        list.forEach(e -> e.setGoodsImage(FileUrlUtil.toAbsoluteIfImage(e.getGoodsImage(), baseServer)));
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verify(String verifyCode, String shopId) {
        if (!StringUtils.hasText(verifyCode)) {
            throw new RuntimeException("核销码不能为空");
        }
        PointsExchange exchange = pointsExchangeMapper.selectOne(new LambdaQueryWrapper<PointsExchange>()
                .eq(PointsExchange::getVerifyCode, verifyCode.trim())
                .last("LIMIT 1"));
        if (exchange == null) {
            throw new RuntimeException("核销码无效");
        }
        // 店铺隔离：只能核销本店的兑换记录
        if (StringUtils.hasText(shopId) && !shopId.equals(exchange.getShopId())) {
            throw new RuntimeException("该核销码不属于本店");
        }
        if (exchange.getStatus() != null && exchange.getStatus() == 2) {
            throw new RuntimeException("该兑换已核销");
        }
        exchange.setStatus(2);
        exchange.setVerifyTime(LocalDateTime.now());
        pointsExchangeMapper.updateById(exchange);
    }

    /** 供外部（会员中心）查询用户积分 */
    public int getUserPoints(String userId) {
        User user = userService.getById(userId);
        return (user == null || user.getPoints() == null) ? 0 : user.getPoints();
    }
}
