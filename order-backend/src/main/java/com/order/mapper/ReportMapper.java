package com.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 经营报表统计（只读）—— 所有查询均按 shopId 隔离
 */
@Mapper
public interface ReportMapper {

    /**
     * 按日期区间统计每日营业额与订单数（不含已取消）
     * 返回: [{ date, amount, orderCount }]
     */
    @Select("<script>SELECT DATE(create_time) AS date, " +
            "IFNULL(SUM(pay_amount), 0) AS amount, " +
            "COUNT(*) AS orderCount " +
            "FROM `order` " +
            "WHERE status &lt;&gt; 3 AND DATE(create_time) BETWEEN #{startDate} AND #{endDate} " +
            "<if test='shopId != null and shopId != \"\"'>AND shop_id = #{shopId} </if>" +
            "GROUP BY DATE(create_time) ORDER BY DATE(create_time) ASC</script>")
    List<Map<String, Object>> dailySummary(@Param("shopId") String shopId,
                                           @Param("startDate") String startDate,
                                           @Param("endDate") String endDate);

    /**
     * 区间汇总：营业额、订单数、客单价
     */
    @Select("<script>SELECT IFNULL(SUM(pay_amount), 0) AS amount, COUNT(*) AS orderCount " +
            "FROM `order` " +
            "WHERE status &lt;&gt; 3 AND DATE(create_time) BETWEEN #{startDate} AND #{endDate} " +
            "<if test='shopId != null and shopId != \"\"'>AND shop_id = #{shopId} </if>" +
            "</script>")
    Map<String, Object> rangeSummary(@Param("shopId") String shopId,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate);

    /**
     * 菜品销量排行（不含已取消订单）
     * 返回: [{ dishName, quantity, amount }]
     */
    @Select("<script>SELECT oi.dish_name AS dishName, " +
            "SUM(oi.quantity) AS quantity, " +
            "SUM(oi.amount) AS amount " +
            "FROM order_item oi " +
            "JOIN `order` o ON oi.order_id = o.id " +
            "WHERE o.status &lt;&gt; 3 AND DATE(o.create_time) BETWEEN #{startDate} AND #{endDate} " +
            "<if test='shopId != null and shopId != \"\"'>AND o.shop_id = #{shopId} </if>" +
            "GROUP BY oi.dish_name " +
            "ORDER BY quantity DESC LIMIT #{limit}</script>")
    List<Map<String, Object>> topDishes(@Param("shopId") String shopId,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("limit") Integer limit);

    /**
     * 按状态统计订单数（区间内）
     */
    @Select("<script>SELECT status, COUNT(*) AS cnt FROM `order` " +
            "WHERE DATE(create_time) BETWEEN #{startDate} AND #{endDate} " +
            "<if test='shopId != null and shopId != \"\"'>AND shop_id = #{shopId} </if>" +
            "GROUP BY status</script>")
    List<Map<String, Object>> statusSummary(@Param("shopId") String shopId,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate);

    /**
     * 按分类统计销量（区间内，不含已取消）
     */
    @Select("<script>SELECT c.name AS categoryName, " +
            "SUM(oi.quantity) AS quantity, " +
            "SUM(oi.amount) AS amount " +
            "FROM order_item oi " +
            "JOIN `order` o ON oi.order_id = o.id " +
            "LEFT JOIN dish d ON oi.dish_id = d.id " +
            "LEFT JOIN category c ON d.category_id = c.id " +
            "WHERE o.status &lt;&gt; 3 AND DATE(o.create_time) BETWEEN #{startDate} AND #{endDate} " +
            "<if test='shopId != null and shopId != \"\"'>AND o.shop_id = #{shopId} </if>" +
            "GROUP BY c.name " +
            "ORDER BY amount DESC</script>")
    List<Map<String, Object>> categorySummary(@Param("shopId") String shopId,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate);
}
