package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.entity.DiningTable;
import com.order.mapper.DiningTableMapper;
import com.order.service.DiningTableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class DiningTableServiceImpl extends ServiceImpl<DiningTableMapper, DiningTable>
        implements DiningTableService {

    @Override
    public List<DiningTable> listByShop(String shopId) {
        // 强制店铺隔离：未指定店铺不返回任何数据（避免泄露全部店铺桌位）
        if (!StringUtils.hasText(shopId)) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getShopId, shopId)
                .orderByAsc(DiningTable::getTableNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTable(DiningTable table, String shopId) {
        if (!StringUtils.hasText(shopId)) {
            throw new RuntimeException("店铺信息缺失，无法新增桌位");
        }
        if (!StringUtils.hasText(table.getTableNo())) {
            throw new RuntimeException("桌号不能为空");
        }
        // 店铺ID由后端强制注入，忽略前端传参
        table.setShopId(shopId);
        if (existsTableNo(shopId, table.getTableNo(), null)) {
            throw new RuntimeException("桌号已存在：" + table.getTableNo());
        }
        if (table.getCapacity() == null) {
            table.setCapacity(4);
        }
        if (table.getStatus() == null) {
            table.setStatus(1);
        }
        save(table);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTable(DiningTable table, String shopId) {
        if (!StringUtils.hasText(table.getId())) {
            throw new RuntimeException("桌位ID不能为空");
        }
        DiningTable exist = getById(table.getId());
        if (exist == null) {
            throw new RuntimeException("桌位不存在");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(exist.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的桌位");
        }
        if (StringUtils.hasText(table.getTableNo())
                && existsTableNo(exist.getShopId(), table.getTableNo(), table.getId())) {
            throw new RuntimeException("桌号已存在：" + table.getTableNo());
        }
        if (StringUtils.hasText(table.getTableNo())) {
            exist.setTableNo(table.getTableNo());
        }
        if (table.getCapacity() != null) {
            exist.setCapacity(table.getCapacity());
        }
        if (table.getQrCode() != null) {
            exist.setQrCode(table.getQrCode());
        }
        if (table.getStatus() != null) {
            exist.setStatus(table.getStatus());
        }
        updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(String id, String shopId) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new RuntimeException("桌位不存在");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(table.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的桌位");
        }
        table.setStatus(table.getStatus() != null && table.getStatus() == 1 ? 0 : 1);
        updateById(table);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(String id, String shopId) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new RuntimeException("桌位不存在");
        }
        if (StringUtils.hasText(shopId) && !shopId.equals(table.getShopId())) {
            throw new RuntimeException("无权操作其他店铺的桌位");
        }
        removeById(id);
    }

    @Override
    public boolean existsTableNo(String shopId, String tableNo, String excludeId) {
        LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getTableNo, tableNo);
        if (StringUtils.hasText(shopId)) {
            wrapper.eq(DiningTable::getShopId, shopId);
        }
        if (StringUtils.hasText(excludeId)) {
            wrapper.ne(DiningTable::getId, excludeId);
        }
        return count(wrapper) > 0;
    }
}
