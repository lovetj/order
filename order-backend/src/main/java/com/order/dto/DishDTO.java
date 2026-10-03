package com.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 菜品新增/编辑 DTO
 */
@Data
public class DishDTO {

    private String id;

    @NotBlank(message = "分类不能为空")
    private String categoryId;

    @NotBlank(message = "菜品名称不能为空")
    private String name;

    /** 对应前端 desc 字段 */
    private String description;

    private String image;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private Integer stock;

    private Integer isHot;

    /** 0下架 1上架 */
    private Integer status;

    private Integer sort;
}
