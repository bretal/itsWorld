package com.itsworld.member.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会员中心方案、礼品与兑换数据访问接口
 *
 * @author Claude
 * @date 2026-11-10
 */
@Mapper
public interface MemberCenterMapper {

    /**
     * 查询会员方案
     *
     * @param activeOnly 是否只查启用方案
     * @return 方案列表
     */
    @Select("<script>SELECT * FROM member_subscription_plan "
            + "<if test='activeOnly'>WHERE status = 1</if> ORDER BY id</script>")
    List<Map<String, Object>> selectPlans(@Param("activeOnly") boolean activeOnly);

    /**
     * 统计启用中的会员等级
     *
     * @param levelId 等级ID
     * @return 匹配数量
     */
    @Select("SELECT COUNT(*) FROM member_level WHERE id = #{levelId} AND status = 1")
    long countActiveLevel(@Param("levelId") Long levelId);

    /**
     * 新增会员方案
     */
    @Insert("INSERT INTO member_subscription_plan(name, level_id, price, duration_days, status) "
            + "VALUES(#{name}, #{levelId}, #{price}, #{durationDays}, #{status})")
    int insertPlan(@Param("name") String name, @Param("levelId") Long levelId,
                   @Param("price") Long price, @Param("durationDays") Integer durationDays,
                   @Param("status") Integer status);

    /**
     * 更新会员方案
     */
    @Update("UPDATE member_subscription_plan SET name = #{name}, level_id = #{levelId}, "
            + "price = #{price}, duration_days = #{durationDays}, status = #{status} WHERE id = #{id}")
    int updatePlan(@Param("id") Long id, @Param("name") String name, @Param("levelId") Long levelId,
                   @Param("price") Long price, @Param("durationDays") Integer durationDays,
                   @Param("status") Integer status);

    /**
     * 查询订单项关联的会员方案ID
     */
    @Select("SELECT target_id FROM commerce_order_item WHERE order_id = #{orderId}")
    Long selectOrderItemTargetId(@Param("orderId") Long orderId);

    /**
     * 查询可购买的会员方案
     */
    @Select("SELECT p.* FROM member_subscription_plan p JOIN member_level l ON l.id = p.level_id "
            + "WHERE p.id = #{planId} AND p.status = 1 AND l.status = 1")
    List<Map<String, Object>> selectPurchasablePlan(@Param("planId") Long planId);

    /**
     * 统计订单已发放的会员权益
     */
    @Select("SELECT COUNT(*) FROM member_subscription_grant WHERE order_id = #{orderId}")
    long countGrantByOrder(@Param("orderId") Long orderId);

    /**
     * 查询会员订单项中的等级与时长快照
     */
    @Select("SELECT JSON_EXTRACT(spec_snapshot, '$.levelId') AS levelId, "
            + "JSON_EXTRACT(spec_snapshot, '$.durationDays') AS days "
            + "FROM commerce_order_item WHERE order_id = #{orderId}")
    Map<String, Object> selectGrantSpec(@Param("orderId") Long orderId);

    /**
     * 新增会员权益发放记录
     */
    @Insert("INSERT INTO member_subscription_grant(order_id, user_id, level_id, starts_at, expires_at) "
            + "VALUES(#{orderId}, #{userId}, #{levelId}, #{startsAt}, #{expiresAt})")
    int insertGrant(@Param("orderId") Long orderId, @Param("userId") Long userId,
                    @Param("levelId") long levelId, @Param("startsAt") LocalDateTime startsAt,
                    @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * 标记会员订单项已履约
     */
    @Update("UPDATE commerce_order_item SET fulfilled_qty = 1 WHERE order_id = #{orderId}")
    int markOrderItemFulfilled(@Param("orderId") Long orderId);

    /**
     * 查询成长值礼品
     *
     * @param activeOnly 是否只查启用礼品
     * @return 礼品列表
     */
    @Select("<script>SELECT * FROM member_reward_product "
            + "<if test='activeOnly'>WHERE status = 1</if> ORDER BY id</script>")
    List<Map<String, Object>> selectRewards(@Param("activeOnly") boolean activeOnly);

    /**
     * 新增成长值礼品
     */
    @Insert("INSERT INTO member_reward_product(name, growth_cost, stock, status) "
            + "VALUES(#{name}, #{growthCost}, #{stock}, #{status})")
    int insertReward(@Param("name") String name, @Param("growthCost") Integer growthCost,
                     @Param("stock") Integer stock, @Param("status") Integer status);

    /**
     * 更新成长值礼品
     */
    @Update("UPDATE member_reward_product SET name = #{name}, growth_cost = #{growthCost}, "
            + "stock = #{stock}, status = #{status} WHERE id = #{id}")
    int updateReward(@Param("id") Long id, @Param("name") String name,
                     @Param("growthCost") Integer growthCost, @Param("stock") Integer stock,
                     @Param("status") Integer status);

    /**
     * 按幂等键查询兑换记录
     */
    @Select("SELECT * FROM member_redemption WHERE user_id = #{userId} AND request_key = #{requestKey}")
    List<Map<String, Object>> selectRedemptionByRequest(@Param("userId") Long userId,
                                                        @Param("requestKey") String requestKey);

    /**
     * 锁定并查询可兑换礼品
     */
    @Select("SELECT * FROM member_reward_product WHERE id = #{productId} AND status = 1 FOR UPDATE")
    List<Map<String, Object>> selectRewardForUpdate(@Param("productId") Long productId);

    /**
     * 扣减礼品库存，库存不足时不更新
     */
    @Update("UPDATE member_reward_product SET stock = stock - 1 WHERE id = #{productId} AND stock > 0")
    int decrementRewardStock(@Param("productId") Long productId);

    /**
     * 新增兑换记录
     */
    @Insert("INSERT INTO member_redemption(redemption_no, user_id, product_id, name_snapshot, "
            + "growth_cost, request_key) VALUES(#{redemptionNo}, #{userId}, #{productId}, "
            + "#{nameSnapshot}, #{growthCost}, #{requestKey})")
    int insertRedemption(@Param("redemptionNo") String redemptionNo, @Param("userId") Long userId,
                         @Param("productId") Long productId, @Param("nameSnapshot") String nameSnapshot,
                         @Param("growthCost") int growthCost, @Param("requestKey") String requestKey);

    /**
     * 查询兑换记录列表
     */
    @Select("<script>SELECT * FROM member_redemption "
            + "<if test='userId != null'>WHERE user_id = #{userId}</if> "
            + "ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<Map<String, Object>> selectRedemptions(@Param("userId") Long userId,
                                                @Param("limit") int limit,
                                                @Param("offset") long offset);

    /**
     * 核销兑换记录
     */
    @Update("UPDATE member_redemption SET status = 'FULFILLED', fulfilled_by = #{adminId}, "
            + "fulfilled_at = NOW() WHERE redemption_no = #{redemptionNo} AND status = 'PENDING'")
    int fulfillRedemption(@Param("redemptionNo") String redemptionNo, @Param("adminId") Long adminId);

    /**
     * 统计已核销的兑换记录
     */
    @Select("SELECT COUNT(*) FROM member_redemption WHERE redemption_no = #{redemptionNo} "
            + "AND status = 'FULFILLED'")
    long countFulfilled(@Param("redemptionNo") String redemptionNo);

}
