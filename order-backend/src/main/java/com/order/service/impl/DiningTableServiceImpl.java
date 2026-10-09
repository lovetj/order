package com.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.config.FileConfigProperties;
import com.order.dto.TableTransferDTO;
import com.order.entity.DiningTable;
import com.order.entity.Order;
import com.order.mapper.DiningTableMapper;
import com.order.mapper.OrderMapper;
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

    @Autowired
    private OrderMapper orderMapper;

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
            if (t.getUseStatus() == null) {
                t.setUseStatus(0);
            }
            if (t.getCurrentOrderCount() == null) {
                t.setCurrentOrderCount(0);
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
        if (table.getUseStatus() == null) {
            table.setUseStatus(0);
        }
        if (table.getCurrentOrderCount() == null) {
            table.setCurrentOrderCount(0);
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
        if (table.getUseStatus() != null) {
            exist.setUseStatus(table.getUseStatus());
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
    @Transactional(rollbackFor = Exception.class)
    public void cleanTable(String id, String shopId) {
        DiningTable table = findTable(id, shopId);
        if (table == null) {
            throw new RuntimeException("桌位不存在");
        }
        // 清台：将桌位使用状态置为空闲(0)，在席订单数置为 0
        table.setUseStatus(0);
        table.setCurrentOrderCount(0);
        updateById(table);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shareTable(String id, String shopId) {
        DiningTable table = findTable(id, shopId);
        if (table == null) {
            throw new RuntimeException("桌位不存在");
        }
        // 拼桌：置为拼桌状态(2)
        table.setUseStatus(2);
        updateById(table);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTable(TableTransferDTO dto, String shopId) {
        if (!StringUtils.hasText(dto.getFromTableNo()) || !StringUtils.hasText(dto.getToTableNo())) {
            throw new RuntimeException("原桌号与目标桌号均不能为空");
        }
        String fromTableNo = dto.getFromTableNo().trim().toUpperCase();
        String toTableNo = dto.getToTableNo().trim().toUpperCase();
        if (fromTableNo.equalsIgnoreCase(toTableNo)) {
            throw new RuntimeException("原桌与目标桌不能相同");
        }

        DiningTable fromTable = findTable(fromTableNo, shopId);
        DiningTable toTable = findTable(toTableNo, shopId);
        if (fromTable == null) {
            throw new RuntimeException("原桌位「" + fromTableNo + "」不存在");
        }
        if (toTable == null) {
            throw new RuntimeException("目标桌位「" + toTableNo + "」不存在");
        }
        if (toTable.getStatus() != null && toTable.getStatus() == 0) {
            throw new RuntimeException("目标桌位「" + toTableNo + "」已停用，无法换桌");
        }

        // 迁移该桌的堂食订单（未完成状态：0待接单 或 1制作中）
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getTableNo, fromTable.getTableNo())
                .eq(Order::getDiningType, 1)
                .in(Order::getStatus, 0, 1);

        if (StringUtils.hasText(dto.getOrderId())) {
            wrapper.and(w -> w.eq(Order::getId, dto.getOrderId()).or().eq(Order::getOrderNo, dto.getOrderId()));
        }

        List<Order> ongoingOrders = orderMapper.selectList(wrapper);
        if (ongoingOrders.isEmpty()) {
            throw new RuntimeException("原桌「" + fromTableNo + "」无进行中的堂食订单可转移");
        }

        for (Order o : ongoingOrders) {
            o.setTableNo(toTable.getTableNo());
            orderMapper.updateById(o);
        }

        // 刷新两桌的状态与在席订单数量
        refreshTableStatus(shopId, fromTable.getTableNo());
        refreshTableStatus(shopId, toTable.getTableNo());
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

    @Override
    public void refreshTableStatus(String shopId, String tableNo) {
        if (!StringUtils.hasText(shopId) || !StringUtils.hasText(tableNo)) {
            return;
        }
        DiningTable table = getOne(new LambdaQueryWrapper<DiningTable>()
                .eq(DiningTable::getShopId, shopId)
                .eq(DiningTable::getTableNo, tableNo).last("LIMIT 1"));
        if (table == null) {
            return;
        }

        // 查询该桌当前进行中（0待接单，1制作中）的堂食订单数
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, shopId)
                .eq(Order::getTableNo, tableNo)
                .eq(Order::getDiningType, 1)
                .in(Order::getStatus, 0, 1));

        int orderCount = count != null ? count.intValue() : 0;
        table.setCurrentOrderCount(orderCount);
        if (orderCount == 0) {
            table.setUseStatus(0); // 空闲
        } else if (orderCount == 1) {
            // 如果原本就是拼桌状态且仍有人在吃，可保留；普通情况为使用中(1)
            table.setUseStatus(table.getUseStatus() != null && table.getUseStatus() == 2 ? 2 : 1);
        } else {
            table.setUseStatus(2); // 拼桌中
        }
        updateById(table);
    }

    private DiningTable findTable(String idOrTableNo, String shopId) {
        if (!StringUtils.hasText(idOrTableNo)) {
            return null;
        }
        DiningTable table = getById(idOrTableNo);
        if (table == null && StringUtils.hasText(shopId)) {
            table = getOne(new LambdaQueryWrapper<DiningTable>()
                    .eq(DiningTable::getShopId, shopId)
                    .eq(DiningTable::getTableNo, idOrTableNo.trim().toUpperCase()).last("LIMIT 1"));
        }
        return table;
    }
}
