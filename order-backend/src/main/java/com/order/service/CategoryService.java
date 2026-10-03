package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /**
     * 查询启用中的分类（顾客端点餐左侧栏），按店铺隔离
     */
    List<Category> listEnabled(String shopId);

    /**
     * 查询全部分类（管理端），按店铺隔离
     */
    List<Category> listAll(String shopId);

    /**
     * 新增分类（校验同一店铺内 code 唯一）
     */
    void addCategory(Category category, String shopId);

    /**
     * 编辑分类（校验归属店铺）
     */
    void updateCategory(Category category, String shopId);

    /**
     * 删除分类（存在菜品时禁止删除，校验归属店铺）
     */
    void deleteCategory(String id, String shopId);
}
