package com.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 换桌 DTO
 */
@Data
public class TableTransferDTO {

    /** 原桌ID 或 桌号 */
    @NotBlank(message = "原桌号不能为空")
    private String fromTableNo;

    /** 目标桌号 */
    @NotBlank(message = "目标桌号不能为空")
    private String toTableNo;

    /** 指定订单ID或订单号（可选，若未指定则转移该桌全部进行中订单） */
    private String orderId;
}
