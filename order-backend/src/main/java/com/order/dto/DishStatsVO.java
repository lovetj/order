package com.order.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理端商品统计：总数 / 在售数 / 各分类明细（供商品管理页头部统计与分类弹框使用）
 */
@Data
public class DishStatsVO {
    /** 商品总数 */
    private Long total;
    /** 在售数量 */
    private Long onShelf;
    /** 各分类统计明细 */
    private List<DishStatItem> categories;
}