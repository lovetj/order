package com.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.common.PageResult;
import com.order.config.FileConfigProperties;
import com.order.dto.DishDTO;
import com.order.dto.DishVO;
import com.order.dto.DishStatItem;
import com.order.dto.DishStatsVO;
import com.order.dto.PageDTO;
import com.order.entity.Dish;
import com.order.mapper.DishMapper;
import com.order.service.DishService;
import com.order.service.DishSpecService;
import com.order.util.FileUrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishSpecService dishSpecService;

    @Autowired
    private FileConfigProperties fileConfigProperties;

    @Override
    public List<DishVO> listForCustomer(String shopId, String categoryId, String keyword) {
        // 只查本店、上架且未删除
        List<Dish> list = baseMapper.selectDishWithCategory(
                shopId, normalizeCategory(categoryId), keyword, 1, 0);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<DishVO> pageForCustomer(String shopId, String categoryId, int pageNum, int pageSize) {
        int num = pageNum > 0 ? pageNum : 1;
        int size = pageSize > 0 ? pageSize : 10;
        Page<Dish> page = new Page<>(num, size);
        // status=1 表示仅上架，isDel=0 表示未删除
        IPage<Dish> result = baseMapper.selectPageCustomer(
                page, shopId, normalizeCategory(categoryId), 1, 0);
        List<DishVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getPages(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<DishVO> pageForAdmin(String shopId, PageDTO pageDTO) {
        PageDTO query = pageDTO != null ? pageDTO : new PageDTO();
        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10;
        Page<Dish> page = new Page<>(pageNum, pageSize);
        IPage<Dish> result = baseMapper.selectPageWithCategory(
                page, shopId, normalizeCategory(query.getCategoryId()), query.getKeyword(), query.getStatus(), query.getIsHot(), 0);
        List<DishVO> records = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getPages(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<DishVO> listForAdmin(String shopId, String categoryId, String keyword, Integer status) {
        List<Dish> list = baseMapper.selectDishWithCategory(
                shopId, normalizeCategory(categoryId), keyword, status, 0);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public DishVO getDetail(String id) {
        // 联表查询带出 categoryName，保证详情页分类名不为空
        Dish dish = baseMapper.selectDishById(id);
        if (dish == null) {
            return null;
        }
        DishVO vo = toVO(dish);
        // 附加规格分组（辣度/加料等）
        vo.setSpecs(dishSpecService.listGroupedByDish(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addDish(DishDTO dto, String shopId) {
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("店铺信息缺失，无法新增菜品");
        }
        Dish dish = new Dish();
        dish.setShopId(shopId);
        dish.setCategoryId(dto.getCategoryId());
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setImage(FileUrlUtil.toRelative(dto.getImage()));
        dish.setPrice(dto.getPrice());
        dish.setMinBuy(dto.getMinBuy());
        dish.setStock(dto.getStock());
        dish.setIsHot(dto.getIsHot());
        dish.setStatus(dto.getStatus());
        dish.setSort(dto.getSort());
        if (dish.getStatus() == null) {
            dish.setStatus(1);
        }
        if (dish.getMinBuy() == null || dish.getMinBuy() < 1) {
            dish.setMinBuy(1);
        }
        if (dish.getStock() == null) {
            dish.setStock(999);
        }
        if (dish.getSales() == null) {
            dish.setSales(0);
        }
        if (dish.getIsHot() == null) {
            dish.setIsHot(0);
        }
        if (dish.getSort() == null) {
            dish.setSort(0);
        }
        dish.setIsDel(0);
        save(dish);
        return dish.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDish(DishDTO dto, String shopId) {
        if (!StringUtils.hasText(dto.getId())) {
            throw new RuntimeException("菜品ID不能为空");
        }
        Dish dish = getById(dto.getId());
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }
        // 归属校验
        if (StringUtils.hasText(shopId) && !shopId.equals(dish.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的菜品");
        }
        // 仅更新非空字段，支持"只改价"这类局部更新
        if (StringUtils.hasText(dto.getCategoryId())) {
            dish.setCategoryId(dto.getCategoryId());
        }
        if (StringUtils.hasText(dto.getName())) {
            dish.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            dish.setDescription(dto.getDescription());
        }
        if (dto.getImage() != null) {
            dish.setImage(FileUrlUtil.toRelative(dto.getImage()));
        }
        if (dto.getPrice() != null) {
            dish.setPrice(dto.getPrice());
        }
        if (dto.getMinBuy() != null) {
            dish.setMinBuy(dto.getMinBuy() < 1 ? 1 : dto.getMinBuy());
        }
        if (dto.getStock() != null) {
            dish.setStock(dto.getStock());
        }
        if (dto.getIsHot() != null) {
            dish.setIsHot(dto.getIsHot());
        }
        if (dto.getStatus() != null) {
            dish.setStatus(dto.getStatus());
        }
        if (dto.getSort() != null) {
            dish.setSort(dto.getSort());
        }
        updateById(dish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, Integer status, String shopId) {
        Dish dish = getById(id);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(dish.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的菜品");
        }
        dish.setStatus(status);
        updateById(dish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDish(String id, String shopId) {
        Dish dish = getById(id);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(dish.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的菜品");
        }
        dish.setIsDel(1);
        dish.setStatus(0);
        updateById(dish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<String> ids, String shopId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            deleteDish(id, shopId);
        }
    }

    /**
     * "all" 视为不筛选分类
     */
    private String normalizeCategory(String categoryId) {
        if (!StringUtils.hasText(categoryId) || "all".equalsIgnoreCase(categoryId)) {
            return null;
        }
        return categoryId;
    }

    /**
     * Entity -> VO：把 desc / status(boolean) 等前端字段对齐
     */
    private DishVO toVO(Dish dish) {
        DishVO vo = new DishVO();
        vo.setId(dish.getId());
        vo.setCategoryId(dish.getCategoryId());
        vo.setCategoryName(dish.getCategoryName());
        vo.setName(dish.getName());
        vo.setDesc(dish.getDescription());
        vo.setPrice(dish.getPrice());
        vo.setMinBuy(dish.getMinBuy() == null || dish.getMinBuy() < 1 ? 1 : dish.getMinBuy());
        vo.setSales(dish.getSales());
        vo.setImage(FileUrlUtil.toAbsoluteIfImage(dish.getImage(), fileConfigProperties.getBaseServer()));
        // 1 上架 -> true
        vo.setStatus(dish.getStatus() != null && dish.getStatus() == 1);
        vo.setStock(dish.getStock());
        vo.setIsHot(dish.getIsHot());
        vo.setSort(dish.getSort());
        return vo;
    }
    @Override
    public DishStatsVO statsForAdmin(String shopId) {
        List<DishStatItem> items = baseMapper.selectCategoryStat(shopId, 0);
        long total = 0, onShelf = 0;
        if (items != null) {
            for (DishStatItem it : items) {
                total += it.getTotal() == null ? 0 : it.getTotal();
                onShelf += it.getOnShelf() == null ? 0 : it.getOnShelf();
            }
        }
        DishStatsVO vo = new DishStatsVO();
        vo.setTotal(total);
        vo.setOnShelf(onShelf);
        vo.setCategories(items);
        return vo;
    }
}
