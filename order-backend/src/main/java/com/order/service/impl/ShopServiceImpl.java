package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.dto.DashboardVO;
import com.order.dto.OrderVO;
import com.order.entity.Order;
import com.order.entity.Shop;
import com.order.mapper.OrderMapper;
import com.order.mapper.ShopMapper;
import com.order.service.OrderService;
import com.order.service.ShopService;
import com.order.config.FileConfigProperties;
import com.order.util.FileUrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> WEEK_DAYS = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FileConfigProperties fileConfigProperties;

    /**
     * 统一整理店铺图片字段后返回给前端：
     * 1. 先把历史脏数据（完整 URL）归一化为相对路径；
     * 2. 再拼接 file.base-server 输出完整地址，前端可直接用于 image src，
     *    也兼容前端 formatImageUrl（其会透传完整地址）。
     * 这样「数据库存相对路径、接口返回完整地址」的约定在前后端保持唯一。
     */
    private Shop normalizeForRead(Shop shop) {
        if (shop == null) {
            return null;
        }
        String baseServer = fileConfigProperties.getBaseServer();
        shop.setLogo(FileUrlUtil.toAbsolute(shop.getLogo(), baseServer));
        shop.setImages(toAbsoluteMulti(shop.getImages(), baseServer));
        return shop;
    }

    /** 逗号分隔的多图字段：逐个归一化并拼接完整地址 */
    private String toAbsoluteMulti(String images, String baseServer) {
        String normalized = FileUrlUtil.normalizeMulti(images);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String item : normalized.split(",")) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(FileUrlUtil.toAbsolute(item, baseServer));
        }
        return sb.toString();
    }

    @Override
    public Shop getShop(String shopId) {
        // 优先按传入的店铺ID精确查询，实现多店铺隔离
        Shop shop = null;
        if (StringUtils.hasText(shopId)) {
            shop = getById(shopId);
        }
        // 兼容：未传 shopId 时取第一家启用门店（单店场景/历史数据兜底）
        if (shop == null && !StringUtils.hasText(shopId)) {
            shop = getOne(new LambdaQueryWrapper<Shop>()
                    .eq(Shop::getStatus, 1)
                    .orderByAsc(Shop::getCreateTime)
                    .last("LIMIT 1"));
        }
        if (shop == null) {
            // 兜底：返回一个默认门店，保证前端不空白
            shop = new Shop();
            shop.setId(StringUtils.hasText(shopId) ? shopId : "default");
            shop.setName("巷子口小馆");
            shop.setSlogan("现点现做 · 用心出餐");
            shop.setNotice("本店支持扫码点餐，出餐后请耐心等待");
            shop.setScore(new BigDecimal("4.9"));
            shop.setMonthSales(0);
            shop.setBusinessStatus(1);
            shop.setStatus(1);
        }
        return normalizeForRead(shop);
    }

    @Override
    public Shop getShopForMerchant(String shopId) {
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("无法识别店铺信息，请重新登录");
        }
        Shop shop = getById(shopId);
        if (shop == null) {
            throw new RuntimeException("门店不存在，请联系平台管理员");
        }
        return normalizeForRead(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShopInfo(String shopId, Shop form) {
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("无法识别店铺信息，请重新登录");
        }
        Shop existing = getById(shopId);
        if (existing == null) {
            throw new RuntimeException("门店不存在，请联系平台管理员");
        }
        if (form == null) {
            throw new RuntimeException("店铺信息不能为空");
        }
        if (!StringUtils.hasText(form.getName())) {
            throw new RuntimeException("店铺名称不能为空");
        }
        if (!StringUtils.hasText(form.getRealName())) {
            throw new RuntimeException("真实姓名不能为空");
        }
        if (!StringUtils.hasText(form.getPhone())) {
            throw new RuntimeException("联系电话不能为空");
        }
        if (!StringUtils.hasText(form.getAddress())) {
            throw new RuntimeException("店铺地址不能为空");
        }

        // 逐字段赋值：仅允许修改基础资料与店铺图片，其余字段（评分/销量/状态等）保持原值
        existing.setName(form.getName().trim());
        existing.setRealName(form.getRealName().trim());
        existing.setSlogan(form.getSlogan());
        existing.setNotice(form.getNotice());
        existing.setAddress(form.getAddress());
        existing.setPhone(form.getPhone().trim());

        // 定位字段：null 表示「前端未改动，保持原值」，避免误清空
        if (form.getLatitude() != null) {
            existing.setLatitude(form.getLatitude());
        }
        if (form.getLongitude() != null) {
            existing.setLongitude(form.getLongitude());
        }

        // 图片字段：null 表示「前端未改动，保持原值」；
        // 只有显式传值（空串=清空，非空=更新）才写入，避免空值覆盖已有图片路径。
        // 统一归一化：兼容前端传完整 URL 或相对路径，数据库始终只存相对路径。
        if (form.getLogo() != null) {
            existing.setLogo(FileUrlUtil.toRelative(form.getLogo()));
        }
        if (form.getImages() != null) {
            existing.setImages(FileUrlUtil.normalizeMulti(form.getImages()));
        }
        log.info("[shop:update] shopId={}, logoIn={}, imagesIn={} -> logoOut={}, imagesOut={}",
                shopId, form.getLogo(), form.getImages(), existing.getLogo(), existing.getImages());

        boolean ok = updateById(existing);
        if (!ok) {
            throw new RuntimeException("店铺信息保存失败，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleBusinessStatus(String shopId) {
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("店铺信息缺失");
        }
        Shop shop = getById(shopId);
        if (shop == null) {
            throw new RuntimeException("门店不存在，请先初始化门店数据");
        }
        Integer next = (shop.getBusinessStatus() != null && shop.getBusinessStatus() == 1) ? 0 : 1;
        shop.setBusinessStatus(next);
        updateById(shop);
    }

    @Override
    public DashboardVO getDashboard(String shopId) {
        DashboardVO vo = new DashboardVO();
        if (!StringUtils.hasText(shopId)) {
            return vo;
        }

        // 1. 概览：今日营业额 / 今日订单 / 客单价 / 待处理（全部限定本店）
        String today = LocalDate.now().format(DATE);
        BigDecimal todayAmount = orderMapper.sumAmountByDate(shopId, today);
        if (todayAmount == null) {
            todayAmount = BigDecimal.ZERO;
        }
        Long todayOrders = orderMapper.countByDate(shopId, today);
        if (todayOrders == null) {
            todayOrders = 0L;
        }
        long pendingCount = orderService.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getStatus, 0));
        long cookingCount = orderService.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getStatus, 1));

        BigDecimal avgAmount = todayOrders > 0
                ? todayAmount.divide(BigDecimal.valueOf(todayOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<DashboardVO.OverviewItem> overview = new ArrayList<>();
        overview.add(new DashboardVO.OverviewItem("今日营业额", "¥" + todayAmount.setScale(2, RoundingMode.HALF_UP), "实时", "primary"));
        overview.add(new DashboardVO.OverviewItem("今日订单", String.valueOf(todayOrders), "实时", "blue"));
        overview.add(new DashboardVO.OverviewItem("客单价", "¥" + avgAmount, "实时", "green"));
        overview.add(new DashboardVO.OverviewItem("待处理", String.valueOf(pendingCount), pendingCount > 0 ? "需关注" : "已处理", "orange"));
        vo.setOverview(overview);

        // 2. 近 7 日营业额
        List<DashboardVO.WeekSale> weekSales = new ArrayList<>();
        BigDecimal max = BigDecimal.ZERO;
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            BigDecimal amount = orderMapper.sumAmountByDate(shopId, date.format(DATE));
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }
            values.add(amount);
            if (amount.compareTo(max) > 0) {
                max = amount;
            }
        }
        for (int i = 0; i < 7; i++) {
            BigDecimal amount = values.get(i);
            int percent = max.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            // 保证最小可见高度
            if (percent == 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
                percent = 5;
            }
            weekSales.add(new DashboardVO.WeekSale(WEEK_DAYS.get(i), amount, percent));
        }
        vo.setWeekSales(weekSales);

        // 3. 待处理订单（待接单 + 制作中，仅本店）
        List<Order> pendingOrders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .in(Order::getStatus, 0, 1)
                .orderByAsc(Order::getCreateTime)
                .last("LIMIT 5"));
        List<OrderVO> pendingVOs = new ArrayList<>();
        for (Order order : pendingOrders) {
            OrderVO detail = orderService.getDetail(order.getId(), shopId);
            if (detail != null) {
                pendingVOs.add(detail);
            }
        }
        vo.setPendingOrders(pendingVOs);

        return vo;
    }
}
