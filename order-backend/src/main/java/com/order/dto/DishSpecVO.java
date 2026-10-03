package com.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品规格 VO —— 已按分组聚合，便于前端渲染
 */
@Data
public class DishSpecVO {

    /** 规格分组，如「辣度」 */
    private String groupName;

    /** 选择类型 1单选 2多选 */
    private Integer selectType;

    /** 是否必选 */
    private Integer required;

    /** 该组下的选项 */
    private List<Option> options;

    @Data
    public static class Option {
        private String id;
        private String name;
        private BigDecimal extraPrice;
        private Boolean isDefault;

        public Option() {
        }

        public Option(String id, String name, BigDecimal extraPrice, Boolean isDefault) {
            this.id = id;
            this.name = name;
            this.extraPrice = extraPrice;
            this.isDefault = isDefault;
        }
    }
}
