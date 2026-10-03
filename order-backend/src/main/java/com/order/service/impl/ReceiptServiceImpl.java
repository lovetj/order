package com.order.service.impl;

import com.order.dto.ReceiptVO;
import com.order.entity.Order;
import com.order.entity.OrderItem;
import com.order.entity.Shop;
import com.order.mapper.OrderItemMapper;
import com.order.mapper.OrderMapper;
import com.order.service.ReceiptService;
import com.order.service.ShopService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** ESC/POS 常用指令 */
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte[] INIT = { ESC, 0x40 };                         // 初始化
    private static final byte[] ALIGN_CENTER = { ESC, 0x61, 0x01 };           // 居中
    private static final byte[] ALIGN_LEFT = { ESC, 0x61, 0x00 };             // 左对齐
    private static final byte[] BOLD_ON = { ESC, 0x45, 0x01 };                // 加粗开
    private static final byte[] BOLD_OFF = { ESC, 0x45, 0x00 };               // 加粗关
    private static final byte[] SIZE_DOUBLE = { GS, 0x21, 0x11 };             // 字体放大一倍
    private static final byte[] SIZE_NORMAL = { GS, 0x21, 0x00 };             // 恢复字号
    private static final byte[] CUT_PAPER = { GS, 0x56, 0x42, 0x00 };         // 切纸

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ShopService shopService;

    @Override
    public ReceiptVO buildReceipt(String orderId) {
        Order order = findOrder(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 小票门店信息取自订单所属店铺，保证多店铺下打印正确门店
        Shop shop = shopService.getShop(order.getShopId());

        ReceiptVO vo = new ReceiptVO();
        vo.setShopName(shop.getName());
        vo.setShopPhone(shop.getPhone());
        vo.setShopAddress(shop.getAddress());

        vo.setOrderNo(order.getOrderNo());
        vo.setPickNo(pickNo(order.getOrderNo()));
        vo.setTableNo(order.getTableNo());
        vo.setPeopleCount(order.getPeopleCount());
        vo.setCreateTime(order.getCreateTime() == null ? "" : order.getCreateTime().format(DATE_TIME));
        vo.setRemark(order.getRemark());
        vo.setStatusText(statusText(order.getStatus()));

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        List<ReceiptVO.ReceiptItem> receiptItems = new ArrayList<>();
        for (OrderItem item : items) {
            receiptItems.add(new ReceiptVO.ReceiptItem(
                    item.getDishName(), item.getQuantity(), item.getPrice(), item.getAmount(),
                    item.getSpecText()));
        }
        vo.setItems(receiptItems);

        vo.setProductTotal(order.getProductTotal());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setText(buildText(vo));
        return vo;
    }

    @Override
    public String buildEscPos(String orderId) {
        ReceiptVO vo = buildReceipt(orderId);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(INIT);
            out.write(ALIGN_CENTER);
            out.write(SIZE_DOUBLE);
            out.write(text(vo.getShopName() + "\n"));
            out.write(SIZE_NORMAL);
            if (notEmpty(vo.getShopPhone())) {
                out.write(text("电话：" + vo.getShopPhone() + "\n"));
            }
            if (notEmpty(vo.getShopAddress())) {
                out.write(text(vo.getShopAddress() + "\n"));
            }
            out.write(text("--------------------------------\n"));
            out.write(ALIGN_LEFT);
            out.write(BOLD_ON);
            out.write(text("取餐号：" + vo.getPickNo() + "\n"));
            out.write(BOLD_OFF);
            out.write(text("订单号：" + vo.getOrderNo() + "\n"));
            out.write(text("桌  号：" + vo.getTableNo() + "\n"));
            if (vo.getPeopleCount() != null) {
                out.write(text("人  数：" + vo.getPeopleCount() + " 人\n"));
            }
            out.write(text("下单时间：" + vo.getCreateTime() + "\n"));
            out.write(text("--------------------------------\n"));
            out.write(text("品名          数量    金额\n"));
            if (vo.getItems() != null) {
                for (ReceiptVO.ReceiptItem item : vo.getItems()) {
                    String name = padRight(item.getName(), 12);
                    String qty = padLeft(String.valueOf(item.getQuantity()), 4);
                    String amt = padLeft(formatMoney(item.getAmount()), 8);
                    out.write(text(name + qty + amt + "\n"));
                    // 规格单独一行缩进显示
                    if (notEmpty(item.getSpecText())) {
                        out.write(text("  (" + item.getSpecText() + ")\n"));
                    }
                }
            }
            out.write(text("--------------------------------\n"));
            out.write(text("小计：" + formatMoney(vo.getProductTotal()) + "\n"));
            if (vo.getDiscountAmount() != null && vo.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                out.write(text("优惠：-" + formatMoney(vo.getDiscountAmount()) + "\n"));
            }
            out.write(BOLD_ON);
            out.write(text("应付：" + formatMoney(vo.getPayAmount()) + "\n"));
            out.write(BOLD_OFF);
            if (notEmpty(vo.getRemark())) {
                out.write(text("备注：" + vo.getRemark() + "\n"));
            }
            out.write(text("--------------------------------\n"));
            out.write(ALIGN_CENTER);
            out.write(text("谢谢惠顾，欢迎再次光临！\n\n\n"));
            out.write(CUT_PAPER);

            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成打印指令失败：" + e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    private Order findOrder(String orderId) {
        // 支持传入订单号或主键ID
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderId).last("LIMIT 1"));
        if (order == null) {
            order = orderMapper.selectById(orderId);
        }
        return order;
    }

    /** 取餐号：订单号后 4 位数字 */
    private String pickNo(String orderNo) {
        if (orderNo == null || orderNo.length() < 4) {
            return orderNo == null ? "" : orderNo;
        }
        return orderNo.substring(orderNo.length() - 4);
    }

    /** 纯文本小票 */
    private String buildText(ReceiptVO vo) {
        StringBuilder sb = new StringBuilder();
        sb.append("        ").append(vo.getShopName()).append("\n");
        if (notEmpty(vo.getShopPhone())) {
            sb.append("电话：").append(vo.getShopPhone()).append("\n");
        }
        sb.append("--------------------------------\n");
        sb.append("取餐号：").append(vo.getPickNo()).append("\n");
        sb.append("订单号：").append(vo.getOrderNo()).append("\n");
        sb.append("桌  号：").append(vo.getTableNo()).append("\n");
        if (vo.getPeopleCount() != null) {
            sb.append("人  数：").append(vo.getPeopleCount()).append(" 人\n");
        }
        sb.append("下单时间：").append(vo.getCreateTime()).append("\n");
        sb.append("--------------------------------\n");
        sb.append("品名          数量    金额\n");
        if (vo.getItems() != null) {
            for (ReceiptVO.ReceiptItem item : vo.getItems()) {
                sb.append(padRight(item.getName(), 12))
                        .append(padLeft(String.valueOf(item.getQuantity()), 4))
                        .append(padLeft(formatMoney(item.getAmount()), 8))
                        .append("\n");
                if (notEmpty(item.getSpecText())) {
                    sb.append("  (").append(item.getSpecText()).append(")\n");
                }
            }
        }
        sb.append("--------------------------------\n");
        sb.append("小计：").append(formatMoney(vo.getProductTotal())).append("\n");
        if (vo.getDiscountAmount() != null && vo.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("优惠：-").append(formatMoney(vo.getDiscountAmount())).append("\n");
        }
        sb.append("应付：").append(formatMoney(vo.getPayAmount())).append("\n");
        if (notEmpty(vo.getRemark())) {
            sb.append("备注：").append(vo.getRemark()).append("\n");
        }
        sb.append("--------------------------------\n");
        sb.append("      谢谢惠顾，欢迎再次光临！\n");
        return sb.toString();
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0: return "待接单";
            case 1: return "制作中";
            case 2: return "已完成";
            case 3: return "已取消";
            default: return "未知";
        }
    }

    private byte[] text(String s) {
        return s.getBytes(java.nio.charset.Charset.forName("GBK"));
    }

    /** 按显示宽度补空格（中文占 2 格） */
    private String padRight(String s, int width) {
        if (s == null) {
            s = "";
        }
        int len = displayWidth(s);
        StringBuilder sb = new StringBuilder(s);
        while (len < width) {
            sb.append(' ');
            len++;
        }
        return sb.toString();
    }

    private String padLeft(String s, int width) {
        if (s == null) {
            s = "";
        }
        int len = displayWidth(s);
        StringBuilder sb = new StringBuilder();
        while (len < width) {
            sb.append(' ');
            len++;
        }
        sb.append(s);
        return sb.toString();
    }

    private int displayWidth(String s) {
        int width = 0;
        for (char c : s.toCharArray()) {
            width += (c > 127) ? 2 : 1;
        }
        return width;
    }

    private String formatMoney(BigDecimal amount) {
        return "¥" + (amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
    }

    private boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
