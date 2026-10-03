package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.dto.DishSpecVO;
import com.order.entity.DishSpec;
import com.order.mapper.DishSpecMapper;
import com.order.service.DishSpecService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DishSpecServiceImpl extends ServiceImpl<DishSpecMapper, DishSpec> implements DishSpecService {

    @Override
    public List<DishSpecVO> listGroupedByDish(String dishId) {
        List<DishSpec> specs = listByDish(dishId);
        // 按分组聚合，保持插入顺序
        Map<String, DishSpecVO> groupMap = new LinkedHashMap<>();
        for (DishSpec spec : specs) {
            String groupName = StringUtils.hasText(spec.getGroupName()) ? spec.getGroupName() : "默认";
            DishSpecVO vo = groupMap.get(groupName);
            if (vo == null) {
                vo = new DishSpecVO();
                vo.setGroupName(groupName);
                vo.setSelectType(spec.getSelectType() == null ? 1 : spec.getSelectType());
                vo.setRequired(spec.getRequired() == null ? 0 : spec.getRequired());
                vo.setOptions(new ArrayList<>());
                groupMap.put(groupName, vo);
            }
            vo.getOptions().add(new DishSpecVO.Option(
                    spec.getId(),
                    spec.getName(),
                    spec.getExtraPrice() == null ? BigDecimal.ZERO : spec.getExtraPrice(),
                    spec.getIsDefault() != null && spec.getIsDefault() == 1
            ));
        }
        return new ArrayList<>(groupMap.values());
    }

    @Override
    public List<DishSpec> listByDish(String dishId) {
        if (!StringUtils.hasText(dishId)) {
            return new ArrayList<>();
        }
        return list(new LambdaQueryWrapper<DishSpec>()
                .eq(DishSpec::getDishId, dishId)
                .eq(DishSpec::getIsDel, 0)
                .eq(DishSpec::getStatus, 1)
                .orderByAsc(DishSpec::getGroupName)
                .orderByAsc(DishSpec::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpecs(String dishId, List<DishSpec> specs, String shopId) {
        if (!StringUtils.hasText(dishId)) {
            throw new RuntimeException("菜品ID不能为空");
        }
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("店铺信息缺失，无法保存规格");
        }
        // 先逻辑删除旧规格
        List<DishSpec> old = list(new LambdaQueryWrapper<DishSpec>()
                .eq(DishSpec::getDishId, dishId)
                .eq(DishSpec::getIsDel, 0));
        for (DishSpec spec : old) {
            spec.setIsDel(1);
            updateById(spec);
        }
        if (specs == null || specs.isEmpty()) {
            return;
        }
        int sort = 0;
        for (DishSpec spec : specs) {
            spec.setId(null);
            spec.setDishId(dishId);
            spec.setShopId(shopId);
            spec.setIsDel(0);
            if (spec.getStatus() == null) {
                spec.setStatus(1);
            }
            if (spec.getSelectType() == null) {
                spec.setSelectType(1);
            }
            if (spec.getExtraPrice() == null) {
                spec.setExtraPrice(BigDecimal.ZERO);
            }
            if (spec.getIsDefault() == null) {
                spec.setIsDefault(0);
            }
            if (spec.getRequired() == null) {
                spec.setRequired(0);
            }
            spec.setSort(sort++);
            save(spec);
        }
    }

    @Override
    public BigDecimal calcAndValidate(String dishId, List<String> specIds) {
        if (specIds == null || specIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<DishSpec> allSpecs = list(new LambdaQueryWrapper<DishSpec>()
                .eq(DishSpec::getDishId, dishId)
                .eq(DishSpec::getIsDel, 0)
                .eq(DishSpec::getStatus, 1));
        Map<String, DishSpec> specMap = new LinkedHashMap<>();
        for (DishSpec spec : allSpecs) {
            specMap.put(spec.getId(), spec);
        }

        BigDecimal extra = BigDecimal.ZERO;
        for (String specId : specIds) {
            DishSpec spec = specMap.get(specId);
            if (spec == null) {
                throw new RuntimeException("所选规格不存在或已停用");
            }
            if (spec.getExtraPrice() != null) {
                extra = extra.add(spec.getExtraPrice());
            }
        }
        return extra;
    }
}
