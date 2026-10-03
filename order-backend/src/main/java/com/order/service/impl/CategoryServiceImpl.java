package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.entity.Category;
import com.order.entity.Dish;
import com.order.mapper.CategoryMapper;
import com.order.mapper.DishMapper;
import com.order.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishMapper dishMapper;

    @Override
    public List<Category> listEnabled(String shopId) {
        return list(new LambdaQueryWrapper<Category>()
                .eq(StringUtils.hasText(shopId), Category::getShopId, shopId)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
    }

    @Override
    public List<Category> listAll(String shopId) {
        return list(new LambdaQueryWrapper<Category>()
                .eq(StringUtils.hasText(shopId), Category::getShopId, shopId)
                .orderByAsc(Category::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCategory(Category category, String shopId) {
        if (!StringUtils.hasText(category.getName())) {
            throw new RuntimeException("分类名称不能为空");
        }
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("店铺信息缺失，无法新增分类");
        }
        // code 未填时用名称兜底生成
        if (!StringUtils.hasText(category.getCode())) {
            category.setCode("c_" + System.currentTimeMillis());
        }
        // code 在同一店铺内唯一
        long exist = count(new LambdaQueryWrapper<Category>()
                .eq(Category::getShopId, shopId)
                .eq(Category::getCode, category.getCode()));
        if (exist > 0) {
            throw new RuntimeException("分类编码已存在：" + category.getCode());
        }
        category.setShopId(shopId);
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Category category, String shopId) {
        if (!StringUtils.hasText(category.getId())) {
            throw new RuntimeException("分类ID不能为空");
        }
        Category exist = getById(category.getId());
        if (exist == null) {
            throw new RuntimeException("分类不存在");
        }
        // 归属校验：不能修改其他店铺的分类
        if (StringUtils.hasText(shopId) && !shopId.equals(exist.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的分类");
        }
        if (StringUtils.hasText(category.getCode())
                && !category.getCode().equals(exist.getCode())) {
            long dup = count(new LambdaQueryWrapper<Category>()
                    .eq(Category::getShopId, exist.getShopId())
                    .eq(Category::getCode, category.getCode())
                    .ne(Category::getId, category.getId()));
            if (dup > 0) {
                throw new RuntimeException("分类编码已存在：" + category.getCode());
            }
        }
        // 店铺ID不可被前端篡改
        category.setShopId(exist.getShopId());
        updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id, String shopId) {
        Category exist = getById(id);
        if (exist == null) {
            throw new RuntimeException("分类不存在");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(exist.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的分类");
        }
        Long dishCount = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getCategoryId, id)
                .eq(Dish::getIsDel, 0));
        if (dishCount != null && dishCount > 0) {
            throw new RuntimeException("该分类下还有 " + dishCount + " 个菜品，请先移除后再删除");
        }
        removeById(id);
    }
}
