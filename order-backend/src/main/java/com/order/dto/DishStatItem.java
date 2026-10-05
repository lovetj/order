package com.order.dto;

import lombok.Data;

/**
 * 分类商品统计项（某分类下的总数量 / 在售数量）
 */
@Data
public class DishStatItem {
    /** 分类ID */
    private String id;
    /** 分类名称 */
    private String name;
    /** 该分类商品总数 */
    private Long total;
    /** 该分类在售数量 */
    private Long onShelf;
}