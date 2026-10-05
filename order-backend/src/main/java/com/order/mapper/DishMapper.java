package com.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.order.entity.Dish;
import com.order.dto.DishStatItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 查询菜品（含分类名称），按店铺隔离
     */
    List<Dish> selectDishWithCategory(@Param("shopId") String shopId,
                                      @Param("categoryId") String categoryId,
                                      @Param("keyword") String keyword,
                                      @Param("status") Integer status,
                                      @Param("isDel") Integer isDel);

    IPage<Dish> selectPageWithCategory(Page<Dish> page,
                                       @Param("shopId") String shopId,
                                       @Param("categoryId") String categoryId,
                                       @Param("keyword") String keyword,
                                       @Param("status") Integer status,
                                       @Param("isDel") Integer isDel);

    /**
     * 按主键查询单个菜品（含分类名称）
     */
    Dish selectDishById(@Param("id") String id);

    /**
     * 按店铺统计各分类的商品总数与在售数量（逻辑删除过滤）
     */
    List<DishStatItem> selectCategoryStat(@Param("shopId") String shopId, @Param("isDel") Integer isDel);
}