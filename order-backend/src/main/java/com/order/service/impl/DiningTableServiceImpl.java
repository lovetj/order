package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.entity.DiningTable;
import com.order.mapper.DiningTableMapper;
import com.order.config.FileConfigProperties;
import com.order.service.DiningTableService;
import com.order.util.FileUrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class DiningTableServiceImpl extends ServiceImpl<DiningTableMapper, DiningTable>
        implements DiningTableService {

    @Autowired
    private FileConfigProperties fileConfigProperties;

    @Override
    public List<DiningTable> listByShop(String shopId) {
        // 强制店铺隔离：未指定店铺不返回任何数据（避免泄露全部店铺桌位）
        if (!StringUtils.hasText(shopId)) {
            return Collections.emptyList();
        }
        List<DiningTable> list = list(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getShopId, shopId)
                .orderByAsc(DiningTable::getBuildingNo)
                .orderByAsc(DiningTable::getType)
                .orderByAsc(DiningTable::getTableNo));
        String baseServer = fileConfigProperties.getBaseServer();
        list.forEach(t -> {
            if (!StringUtils.hasText(t.getBuildingNo())) {
                t.setBuildingNo("1楼");
            }
            if (!StringUtils.hasText(t.getType())) {
                t.setType("大厅");
            }
            t.setQrCode(FileUrlUtil.toAbsoluteIfImage(t.getQrCode(), baseServer));
        });
        return list;
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
        if (existsTableNo(shopId, table.getTableNo().trim().toUpperCase(), null)) {
            throw new RuntimeException("桌号已存在：" + table.getTableNo());
        }
        table.setTableNo(table.getTableNo().trim().toUpperCase());
        table.setBuildingNo(StringUtils.hasText(table.getBuildingNo()) ? table.getBuildingNo().trim() : "1楼");
        table.setType(StringUtils.hasText(table.getType()) ? table.getType().trim() : "大厅");
        table.setAlias(StringUtils.hasText(table.getAlias()) ? table.getAlias().trim() : null);

        if (table.getCapacity() == null || table.getCapacity() < 1) {
            table.setCapacity(4);
        }
        if (table.getStatus() == null) {
            table.setStatus(1);
        }
        if (table.getQrCode() != null) {
            table.setQrCode(FileUrlUtil.toRelative(table.getQrCode()));
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
        if (StringUtils.hasText(table.getTableNo())) {
            String newTableNo = table.getTableNo().trim().toUpperCase();
            if (existsTableNo(exist.getShopId(), newTableNo, table.getId())) {
                throw new RuntimeException("桌号已存在：" + newTableNo);
            }
            exist.setTableNo(newTableNo);
        }
        if (table.getBuildingNo() != null) {
            exist.setBuildingNo(StringUtils.hasText(table.getBuildingNo()) ? table.getBuildingNo().trim() : "1楼");
        }
        if (table.getType() != null) {
            exist.setType(StringUtils.hasText(table.getType()) ? table.getType().trim() : "大厅");
        }
        if (table.getAlias() != null) {
            exist.setAlias(StringUtils.hasText(table.getAlias()) ? table.getAlias().trim() : null);
        }
        if (table.getCapacity() != null) {
            exist.setCapacity(table.getCapacity() >= 1 ? table.getCapacity() : 1);
        }
        if (table.getQrCode() != null) {
            exist.setQrCode(FileUrlUtil.toRelative(table.getQrCode()));
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
