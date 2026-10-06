package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.common.PageResult;
import com.order.dto.DishDTO;
import com.order.dto.DishVO;
import com.order.dto.DishStatsVO;
import com.order.dto.PageDTO;
import com.order.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    /**
     * 顾客端菜品列表（仅上架），字段已转换为前端格式，按店铺隔离
     *
     * @param shopId     所属店铺
     * @param categoryId 分类ID，为空或 all 表示全部
     */
    List<DishVO> listForCustomer(String shopId, String categoryId, String keyword);

    /**
     * 顾客端菜品分页列表（仅上架，按销量排序），按店铺隔离
     *
     * @param shopId     所属店铺
     * @param categoryId 分类ID，为空或 all 表示全部
     */
    PageResult<DishVO> pageForCustomer(String shopId, String categoryId, int pageNum, int pageSize);

    /**
     * 管理端菜品分页列表（含下架），按店铺隔离
     */
    PageResult<DishVO> pageForAdmin(String shopId, PageDTO pageDTO);

    /**
     * 管理端菜品列表，按店铺隔离
     */
    List<DishVO> listForAdmin(String shopId, String categoryId, String keyword, Integer status);

    /**
     * 菜品详情
     */
    DishVO getDetail(String id);

    /**
     * 新增菜品
     *
     * @return 新增菜品的ID（供前端继续保存规格）
     */
    String addDish(DishDTO dto, String shopId);

    /**
     * 编辑菜品（校验归属店铺）
     */
    void updateDish(DishDTO dto, String shopId);

    /**
     * 上下架切换（校验归属店铺）
     */
    void updateStatus(String id, Integer status, String shopId);

    /**
     * 删除菜品（逻辑删除，校验归属店铺）
     */
    void deleteDish(String id, String shopId);

    /**
     * 批量删除（校验归属店铺）
     */
    void deleteBatch(List<String> ids, String shopId);

    /**
     * 管理端商品统计（总数 / 在售 / 各分类）
     */
    DishStatsVO statsForAdmin(String shopId);
}