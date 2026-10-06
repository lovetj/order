package com.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 按状态分组统计订单数量（顾客维度，跨店铺统计该用户全部订单）
     */
    @Select("<script>SELECT status, COUNT(*) AS cnt FROM `order` WHERE user_id = #{userId} " +
            "<if test='shopId != null and shopId != \"\"'>AND shop_id = #{shopId}</if> " +
            "GROUP BY status</script>")
    List<Map<String, Object>> countGroupByStatus(@Param("userId") String userId, @Param("shopId") String shopId);

    /**
     * 统计某天的营业额（不含已取消），按店铺隔离
     */
    @Select("<script>SELECT IFNULL(SUM(pay_amount), 0) FROM `order` " +
            "WHERE DATE(create_time) = #{date} AND status &lt;&gt; 3 " +
            "<if test='shopId != null and shopId != \"\"'>AND shop_id = #{shopId} </if>" +
            "</script>")
    BigDecimal sumAmountByDate(@Param("shopId") String shopId, @Param("date") String date);

    /**
     * 统计某天的订单数（不含已取消），按店铺隔离
     */
    @Select("<script>SELECT COUNT(*) FROM `order` " +
            "WHERE DATE(create_time) = #{date} AND status &lt;&gt; 3 " +
            "<if test='shopId != null and shopId != \"\"'>AND shop_id = #{shopId} </if>" +
            "</script>")
    Long countByDate(@Param("shopId") String shopId, @Param("date") String date);
}
