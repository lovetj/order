package com.order.controller;

import com.order.common.Result;
import com.order.dto.ReceiptVO;
import com.order.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 小票打印 —— 店家端
 */
@RestController
@RequestMapping("/api/receipt")
public class ReceiptController {

    @Autowired
    private ReceiptService receiptService;

    /** 获取小票数据（含纯文本，供前端预览/云打印） */
    @GetMapping("/{orderId}")
    public Result<ReceiptVO> detail(@PathVariable String orderId) {
        return Result.success(receiptService.buildReceipt(orderId));
    }

    /** 获取 ESC/POS 打印指令（Base64），可透传给热敏打印机 */
    @GetMapping("/{orderId}/escpos")
    public Result<Map<String, String>> escpos(@PathVariable String orderId) {
        Map<String, String> data = new HashMap<>();
        data.put("command", receiptService.buildEscPos(orderId));
        data.put("encoding", "base64");
        data.put("charset", "GBK");
        return Result.success(data);
    }
}
