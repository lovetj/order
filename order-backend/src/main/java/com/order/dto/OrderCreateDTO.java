package com.order.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 顾客下单 DTO（扫码点餐）
 */
@Data
public class OrderCreateDTO {

    /** 桌号 */
    @NotNull(message = "桌号不能为空")
    private String tableNo;

    /** 就餐人数 */
    private Integer peopleCount;

    /** 菜品明细 */
    @NotEmpty(message = "请先选择菜品")
    private List<OrderItemDTO> items;

    /** 备注 */
    private String remark;

    /** 使用的用户优惠券ID（可选） */
    private String userCouponId;

    /** 使用的积分（可选，100 积分抵 1 元） */
    private Integer usePoints;

    @Data
    public static class OrderItemDTO {
        private String dishId;
        private Integer quantity;
        /** 选中的规格选项ID列表（辣度/加料等，可为空） */
        private List<String> specIds;
        /** 规格描述快照，如「微辣, 加蛋」，前端可直接传 */
        private String specText;
    }
}
