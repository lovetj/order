package com.order.service;

import com.order.dto.ReceiptVO;

public interface ReceiptService {

    /**
     * 构建订单小票数据
     */
    ReceiptVO buildReceipt(String orderId);

    /**
     * 生成 ESC/POS 指令（字节流，Base64 编码返回）
     * 适用于热敏小票打印机（网口/串口/蓝牙均可透传该指令）
     */
    String buildEscPos(String orderId);
}
