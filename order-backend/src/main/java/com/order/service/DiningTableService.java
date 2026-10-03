package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.entity.DiningTable;

import java.util.List;

public interface DiningTableService extends IService<DiningTable> {

    /**
     * 查询门店全部桌位（必须传入 shopId，强制店铺隔离）
     */
    List<DiningTable> listByShop(String shopId);

    /**
     * 新增桌位（店铺ID由后端从登录态注入，不信任前端）
     */
    void addTable(DiningTable table, String shopId);

    /**
     * 编辑桌位（校验归属店铺）
     */
    void updateTable(DiningTable table, String shopId);

    /**
     * 切换启用/停用（校验归属店铺）
     */
    void toggleStatus(String id, String shopId);

    /**
     * 删除桌位（校验归属店铺）
     */
    void deleteTable(String id, String shopId);

    /**
     * 校验桌号是否存在（同店内唯一）
     */
    boolean existsTableNo(String shopId, String tableNo, String excludeId);
}
