package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.dto.DishSpecVO;
import com.order.entity.DishSpec;

import java.util.List;

public interface DishSpecService extends IService<DishSpec> {

    /**
     * 查询某菜品的全部规格（按分组聚合，供顾客端选择）
     */
    List<DishSpecVO> listGroupedByDish(String dishId);

    /**
     * 查询某菜品的规格明细（管理端编辑）
     */
    List<DishSpec> listByDish(String dishId);

    /**
     * 批量保存菜品规格（先删后插，管理端整体提交）
     *
     * @param shopId 规格所属店铺（写入每条规格，实现店铺隔离）
     */
    void saveSpecs(String dishId, List<DishSpec> specs, String shopId);

    /**
     * 校验顾客选择的规格是否合法，并计算加价合计
     *
     * @param dishId  菜品ID
     * @param specIds 顾客选中的规格选项ID
     * @return 规格加价合计
     */
    java.math.BigDecimal calcAndValidate(String dishId, List<String> specIds);
}
