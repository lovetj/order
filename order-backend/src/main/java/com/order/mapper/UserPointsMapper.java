package com.order.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户积分账本（按商家隔离）
 *
 * 与全局 user.points（旧字段，不再作为多店铺积分依据）不同，
 * 本表以 (user_id, shop_id) 为复合主键，为每个用户在每家店铺维护独立积分余额。
 */
@Mapper
public interface UserPointsMapper {

    /** 增加积分：账本不存在则新建，存在则累加（复合主键冲突走累加） */
    @Insert("INSERT INTO `user_points` (`user_id`, `shop_id`, `points`, `create_time`, `update_time`) "
            + "VALUES (#{userId}, #{shopId}, #{points}, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE `points` = `points` + #{points}, `update_time` = NOW()")
    int addPoints(@Param("userId") String userId,
                  @Param("shopId") String shopId,
                  @Param("points") int points);

    /** 扣减积分：余额充足才扣，返回 1 表示成功、0 表示余额不足或账本不存在 */
    @Update("UPDATE `user_points` SET `points` = `points` - #{points}, `update_time` = NOW() "
            + "WHERE `user_id` = #{userId} AND `shop_id` = #{shopId} AND `points` >= #{points}")
    int deductEnough(@Param("userId") String userId,
                     @Param("shopId") String shopId,
                     @Param("points") int points);

    /** 查询用户在指定店铺的积分余额，不存在返回 null */
    @Select("SELECT `points` FROM `user_points` "
            + "WHERE `user_id` = #{userId} AND `shop_id` = #{shopId} LIMIT 1")
    Integer selectPoints(@Param("userId") String userId, @Param("shopId") String shopId);
}