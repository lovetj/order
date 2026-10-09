package com.order.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.common.PageResult;
import com.order.config.FileConfigProperties;
import com.order.dto.OrderCreateDTO;
import com.order.dto.OrderItemVO;
import com.order.dto.OrderVO;
import com.order.entity.Dish;
import com.order.entity.Order;
import com.order.entity.OrderItem;
import com.order.mapper.DishMapper;
import com.order.mapper.OrderItemMapper;
import com.order.mapper.OrderMapper;
import com.order.service.CouponService;
import com.order.service.DiningTableService;
import com.order.service.MemberService;
import com.order.service.OrderService;
import com.order.util.FileUrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 状态：0待接单 1制作中 2已完成 3已取消 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_COOKING = 1;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_CANCELED = 3;

    /** 就餐方式：1堂食 2外带 */
    public static final int DINING_TYPE_DINE_IN = 1;
    public static final int DINING_TYPE_TAKEOUT = 2;

    /** 积分抵扣比例：100 积分抵 1 元 */
    private static final int POINTS_PER_YUAN_DEDUCT = 100;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CouponService couponService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private com.order.service.DishSpecService dishSpecService;

    @Autowired
    private FileConfigProperties fileConfigProperties;

    @Autowired
    @Lazy
    private DiningTableService diningTableService;

    @Autowired
    private com.order.mapper.UserMapper userMapper;

    // ==================== 顾客下单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderCreateDTO dto, String userId, String shopId) {
        if (!StringUtils.hasText(userId)) {
            throw new RuntimeException("用户未登录");
        }
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("未识别店铺信息，请重新扫描桌位二维码或选择店铺");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("请先选择菜品");
        }

        int diningType = dto.getDiningType() != null && dto.getDiningType() == DINING_TYPE_TAKEOUT
                ? DINING_TYPE_TAKEOUT : DINING_TYPE_DINE_IN;

        String tableNo = dto.getTableNo();
        if (diningType == DINING_TYPE_DINE_IN) {
            if (!StringUtils.hasText(tableNo)) {
                throw new RuntimeException("堂食下单桌号不能为空");
            }
            tableNo = tableNo.trim().toUpperCase();
        } else {
            // 外带不强制要求物理桌号，不占用桌位
            if (!StringUtils.hasText(tableNo)) {
                tableNo = "TAKEOUT";
            }
        }

        // 1. 批量查询菜品，校验上架与库存
        List<String> dishIds = dto.getItems().stream()
                .map(OrderCreateDTO.OrderItemDTO::getDishId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (dishIds.isEmpty()) {
            throw new RuntimeException("请先选择菜品");
        }
        Map<String, Dish> dishMap = dishMapper.selectBatchIds(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, d -> d, (a, b) -> a));

        BigDecimal productTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderCreateDTO.OrderItemDTO item : dto.getItems()) {
            Dish dish = dishMap.get(item.getDishId());
            if (dish == null) {
                throw new RuntimeException("菜品不存在或已下架");
            }
            // 店铺隔离：所选菜品必须属于当前店铺
            if (StringUtils.hasText(dish.getShopId()) && !shopId.equals(dish.getShopId())) {
                throw new RuntimeException("菜品「" + dish.getName() + "」不属于当前店铺");
            }
            if (dish.getIsDel() != null && dish.getIsDel() == 1) {
                throw new RuntimeException("菜品已删除：" + dish.getName());
            }
            if (dish.getStatus() == null || dish.getStatus() != 1) {
                throw new RuntimeException("菜品已下架：" + dish.getName());
            }
            int quantity = item.getQuantity() == null || item.getQuantity() <= 0 ? 1 : item.getQuantity();
            // 起购份数校验：数量不得低于菜品设定的起购份数
            int minBuy = dish.getMinBuy() == null || dish.getMinBuy() < 1 ? 1 : dish.getMinBuy();
            if (quantity < minBuy) {
                throw new RuntimeException("「" + dish.getName() + "」" + minBuy + " 份起购");
            }
            if (dish.getStock() != null && quantity > dish.getStock()) {
                throw new RuntimeException("「" + dish.getName() + "」库存不足，仅剩 " + dish.getStock());
            }

            // 规格校验与加价
            BigDecimal specPrice = BigDecimal.ZERO;
            if (item.getSpecIds() != null && !item.getSpecIds().isEmpty()) {
                specPrice = dishSpecService.calcAndValidate(dish.getId(), item.getSpecIds());
            }

            BigDecimal unitPrice = dish.getPrice().add(specPrice);
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            productTotal = productTotal.add(amount);

            OrderItem oi = new OrderItem();
            oi.setShopId(shopId);
            oi.setDishId(dish.getId());
            oi.setDishName(dish.getName());
            oi.setDishImage(dish.getImage());
            oi.setPrice(unitPrice);
            oi.setQuantity(quantity);
            oi.setAmount(amount);
            oi.setSpecText(item.getSpecText());
            oi.setSpecPrice(specPrice);
            orderItems.add(oi);
        }

        // 2. 创建订单（先落库，便于优惠券关联 orderId）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setTableNo(tableNo);
        order.setDiningType(diningType);
        order.setPeopleCount(dto.getPeopleCount());
        order.setProductTotal(productTotal);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(productTotal);
        order.setPayType(99); // 未支付
        order.setStatus(STATUS_PENDING);
        order.setRemark(dto.getRemark());
        save(order);

        // 3. 优惠券抵扣
        BigDecimal discount = BigDecimal.ZERO;
        if (StringUtils.hasText(dto.getUserCouponId())) {
            discount = couponService.useCoupon(dto.getUserCouponId(), order.getId(), userId, productTotal, shopId);
        }

        // 4. 积分抵扣（100 积分 = 1 元）
        BigDecimal pointsDeduction = BigDecimal.ZERO;
        if (dto.getUsePoints() != null && dto.getUsePoints() > 0) {
            BigDecimal maxUsable = productTotal.subtract(discount);
            if (maxUsable.compareTo(BigDecimal.ZERO) < 0) {
                maxUsable = BigDecimal.ZERO;
            }
            int maxPoints = maxUsable.multiply(BigDecimal.valueOf(POINTS_PER_YUAN_DEDUCT))
                    .setScale(0, RoundingMode.DOWN).intValue();
            int usePoints = Math.min(dto.getUsePoints(), maxPoints);
            if (usePoints > 0) {
                memberService.deductPoints(userId, shopId, usePoints);
                pointsDeduction = BigDecimal.valueOf(usePoints)
                        .divide(BigDecimal.valueOf(POINTS_PER_YUAN_DEDUCT), 2, RoundingMode.HALF_UP);
            }
        }

        // 5. 计算实付并回写
        BigDecimal payAmount = productTotal.subtract(discount).subtract(pointsDeduction);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }
        order.setDiscountAmount(discount.add(pointsDeduction));
        order.setPayAmount(payAmount);
        updateById(order);

        // 6. 保存明细 + 扣减库存/累加销量
        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderItemMapper.insert(oi);

            Dish dish = dishMap.get(oi.getDishId());
            if (dish != null) {
                if (dish.getStock() != null) {
                    dish.setStock(Math.max(0, dish.getStock() - oi.getQuantity()));
                }
                dish.setSales((dish.getSales() == null ? 0 : dish.getSales()) + oi.getQuantity());
                dishMapper.updateById(dish);
            }
        }

        // 7. 如果是堂食，联动刷新餐桌使用状态；外带则不占用桌位
        if (diningType == DINING_TYPE_DINE_IN && StringUtils.hasText(tableNo)) {
            diningTableService.refreshTableStatus(shopId, tableNo);
        }

        return getDetail(order.getId(), shopId);
    }

    // ==================== 查询 ====================

    @Override
    public PageResult<OrderVO> pageForCustomer(Integer pageNum, Integer pageSize, String status, String userId, String shopId) {
        return pageOrders(pageNum, pageSize, status, userId, shopId, null);
    }

    @Override
    public PageResult<OrderVO> pageForMerchant(String shopId, Integer pageNum, Integer pageSize, String status, Integer diningType) {
        return pageOrders(pageNum, pageSize, status, null, shopId, diningType);
    }

    private PageResult<OrderVO> pageOrders(Integer pageNum, Integer pageSize, String status,
                                           String userId, String shopId, Integer diningType) {
        int num = pageNum != null && pageNum > 0 ? pageNum : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userId)) {
            wrapper.eq(Order::getUserId, userId);
        }
        // 店铺隔离：店家端只能看到本店订单
        if (StringUtils.hasText(shopId)) {
            wrapper.eq(Order::getShopId, shopId);
        }
        Integer statusCode = toStatusCode(status);
        if (statusCode != null) {
            wrapper.eq(Order::getStatus, statusCode);
        }
        if (diningType != null && diningType > 0) {
            wrapper.eq(Order::getDiningType, diningType);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = new Page<>(num, size);
        page(page, wrapper);
        List<Order> orders = page.getRecords();
        Set<String> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, String> userPhoneMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<com.order.entity.User> users = userMapper.selectBatchIds(userIds);
            if (users != null) {
                for (com.order.entity.User u : users) {
                    if (u != null && u.getId() != null) {
                        userPhoneMap.put(u.getId(), u.getPhone());
                    }
                }
            }
        }
        List<OrderVO> records = orders.stream()
                .map(o -> this.toVOWithItems(o, userPhoneMap.get(o.getUserId())))
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getPages(), page.getCurrent(), page.getSize());
    }

    @Override
    public OrderVO getDetail(String id, String shopId) {
        Order order = findOrder(id);
        if (order == null) {
            return null;
        }
        // 店铺隔离：顾客只能查看当前店铺的订单
        if (StringUtils.hasText(shopId) && !shopId.equals(order.getShopId())) {
            return null;
        }
        return toVOWithItems(order);
    }

    private Order findOrder(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        Order order = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, id).last("LIMIT 1"));
        if (order == null) {
            order = getById(id);
        }
        return order;
    }

    @Override
    public Map<String, Long> countByUser(String userId, String shopId) {
        Map<String, Long> result = new HashMap<>();
        result.put("pending", 0L);
        result.put("cooking", 0L);
        result.put("done", 0L);
        result.put("canceled", 0L);
        result.put("all", 0L);
        if (!StringUtils.hasText(userId)) {
            return result;
        }
        List<Map<String, Object>> rows = baseMapper.countGroupByStatus(userId, shopId);
        long total = 0L;
        for (Map<String, Object> row : rows) {
            Integer st = row.get("status") == null ? null : ((Number) row.get("status")).intValue();
            long cnt = row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue();
            total += cnt;
            String key = toStatusString(st);
            if (key != null) {
                result.put(key, cnt);
            }
        }
        result.put("all", total);
        return result;
    }

    @Override
    public Map<String, Long> countForMerchant(String shopId) {
        Map<String, Long> result = new HashMap<>();
        if (!StringUtils.hasText(shopId)) {
            result.put("pending", 0L);
            result.put("cooking", 0L);
            result.put("done", 0L);
            result.put("all", 0L);
            return result;
        }
        long pending = count(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId).eq(Order::getStatus, STATUS_PENDING));
        long cooking = count(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId).eq(Order::getStatus, STATUS_COOKING));
        long done = count(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId).eq(Order::getStatus, STATUS_DONE));
        result.put("pending", pending);
        result.put("cooking", cooking);
        result.put("done", done);
        result.put("all", pending + cooking + done);
        return result;
    }

    // ==================== 状态流转 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(String id, String shopId) {
        Order order = requireOwnedOrder(id, shopId);
        if (order.getStatus() != STATUS_PENDING) {
            throw new RuntimeException("当前订单状态不可接单");
        }
        order.setStatus(STATUS_COOKING);
        order.setAcceptTime(LocalDateTime.now());
        updateById(order);

        // 联动餐桌状态
        if (order.getDiningType() != null && order.getDiningType() == DINING_TYPE_DINE_IN && StringUtils.hasText(order.getTableNo())) {
            diningTableService.refreshTableStatus(shopId, order.getTableNo());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(String id, String shopId) {
        Order order = requireOwnedOrder(id, shopId);
        if (order.getStatus() != STATUS_COOKING) {
            throw new RuntimeException("当前订单状态不可出餐");
        }
        order.setStatus(STATUS_DONE);
        order.setFinishTime(LocalDateTime.now());
        updateById(order);

        // 订单完成：累加会员积分与消费额、刷新等级
        memberService.addOrderReward(order.getUserId(), order.getShopId(), order.getPayAmount());

        // 联动餐桌状态刷新
        if (order.getDiningType() != null && order.getDiningType() == DINING_TYPE_DINE_IN && StringUtils.hasText(order.getTableNo())) {
            diningTableService.refreshTableStatus(shopId, order.getTableNo());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String id, String reason, String shopId) {
        Order order = requireOwnedOrder(id, shopId);
        if (order.getStatus() == STATUS_DONE || order.getStatus() == STATUS_CANCELED) {
            throw new RuntimeException("当前订单状态不可拒单");
        }
        order.setStatus(STATUS_CANCELED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(StringUtils.hasText(reason) ? reason : "店家拒单");
        updateById(order);
        // 退回已使用的优惠券
        couponService.refundCoupon(order.getId());

        // 联动餐桌状态刷新
        if (order.getDiningType() != null && order.getDiningType() == DINING_TYPE_DINE_IN && StringUtils.hasText(order.getTableNo())) {
            diningTableService.refreshTableStatus(shopId, order.getTableNo());
        }
    }

    private Order requireOwnedOrder(String id, String shopId) {
        Order order = findOrder(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(order.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的订单");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String id, String userId, String reason, String shopId) {
        Order order = findOrder(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (StringUtils.hasText(userId) && !userId.equals(order.getUserId())) {
            throw new RuntimeException("无权操作该订单");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(order.getShopId())) {
            throw new RuntimeException("无权操作该订单");
        }
        if (order.getStatus() == STATUS_DONE || order.getStatus() == STATUS_CANCELED) {
            throw new RuntimeException("当前订单状态不可取消");
        }
        order.setStatus(STATUS_CANCELED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(StringUtils.hasText(reason) ? reason : "顾客取消");
        updateById(order);
        couponService.refundCoupon(order.getId());

        if (order.getDiningType() != null && order.getDiningType() == DINING_TYPE_DINE_IN && StringUtils.hasText(order.getTableNo())) {
            diningTableService.refreshTableStatus(order.getShopId(), order.getTableNo());
        }
    }

    // ==================== VO 转换 ====================

    private OrderVO toVOWithItems(Order order) {
        return toVOWithItems(order, null);
    }

    private OrderVO toVOWithItems(Order order, String phone) {
        OrderVO vo = toVO(order, phone);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    private OrderVO toVO(Order order) {
        return toVO(order, null);
    }

    private OrderVO toVO(Order order, String phone) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getOrderNo() != null ? order.getOrderNo() : order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTable(order.getTableNo());
        int diningType = order.getDiningType() != null ? order.getDiningType() : DINING_TYPE_DINE_IN;
        vo.setDiningType(diningType);
        vo.setDiningTypeText(diningType == DINING_TYPE_TAKEOUT ? "外带" : "堂食");
        vo.setStatus(toStatusString(order.getStatus()));
        vo.setStatusText(toStatusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime() == null ? null : order.getCreateTime().format(DATE_TIME));
        vo.setAmount(order.getPayAmount());
        vo.setProductTotal(order.getProductTotal());
        vo.setPeopleCount(order.getPeopleCount());
        vo.setRemark(order.getRemark());
        vo.setAction(toAction(order.getStatus()));
        if (StringUtils.hasText(phone)) {
            vo.setPhone(phone);
            vo.setUserPhone(phone);
        } else if (StringUtils.hasText(order.getUserId()) && userMapper != null) {
            com.order.entity.User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                vo.setPhone(user.getPhone());
                vo.setUserPhone(user.getPhone());
            }
        }
        return vo;
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setDishId(item.getDishId());
        vo.setName(item.getDishName());
        vo.setCount(item.getQuantity());
        vo.setPrice(item.getPrice());
        vo.setAmount(item.getAmount());
        vo.setImage(FileUrlUtil.toAbsoluteIfImage(item.getDishImage(), fileConfigProperties.getBaseServer()));
        vo.setSpecText(item.getSpecText());
        return vo;
    }

    private String toStatusString(Integer status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case STATUS_PENDING:
                return "pending";
            case STATUS_COOKING:
                return "cooking";
            case STATUS_DONE:
                return "done";
            case STATUS_CANCELED:
                return "canceled";
            default:
                return "unknown";
        }
    }

    private String toStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待接单";
            case STATUS_COOKING:
                return "制作中";
            case STATUS_DONE:
                return "已完成";
            case STATUS_CANCELED:
                return "已取消";
            default:
                return "未知";
        }
    }

    private Integer toStatusCode(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        switch (status.toLowerCase()) {
            case "pending":
                return STATUS_PENDING;
            case "cooking":
                return STATUS_COOKING;
            case "done":
                return STATUS_DONE;
            case "canceled":
                return STATUS_CANCELED;
            default:
                return null;
        }
    }

    private OrderVO.Action toAction(Integer status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case STATUS_PENDING:
                return new OrderVO.Action("接单制作", "cooking", "制作中");
            case STATUS_COOKING:
                return new OrderVO.Action("出餐完成", "done", "已完成");
            default:
                return null;
        }
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "D" + dateStr + RandomUtil.randomNumbers(3);
    }
}
