package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品 VO —— 字段与前端 utils/data.js 的 GOODS 完全对齐
 * { id, categoryId, name, desc, price, sales, image, status }
 * 另有管理端需要的 stock / categoryName / isHot
 */
@Data
public class DishVO {
    private String id;
    private String categoryId;
    private String categoryName;
    private String name;
    /** 前端使用 desc 字段 */
    private String desc;
    private BigDecimal price;
    private Integer sales;
    private String image;
    /** 前端 status 为布尔值：true 上架 / false 下架 */
    private Boolean status;
    private Integer stock;
    private Integer isHot;
    private Integer sort;
    /** 规格分组（详情接口返回） */
    private java.util.List<DishSpecVO> specs;
}
