package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.dto.CouponView;
import com.itsworld.member.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户优惠券数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 查询用户券列表（带模板信息）
     *
     * @param userId 用户ID
     * @param status 状态过滤，可为空
     * @return 用户券列表
     */
    @Select("<script>"
            + "SELECT uc.id, uc.coupon_code, ct.template_name, ct.coupon_type, ct.amount_cents, "
            + "ct.discount_rate, ct.min_amount_cents, ct.description, uc.status, "
            + "uc.valid_from, uc.valid_to, uc.used_at, uc.created_at "
            + "FROM user_coupon uc "
            + "JOIN coupon_template ct ON ct.id = uc.template_id "
            + "WHERE uc.user_id = #{userId} "
            + "<if test='status != null and status != \"\"'> AND uc.status = #{status}</if> "
            + "ORDER BY uc.id DESC"
            + "</script>")
    List<CouponView> selectUserCoupons(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 锁定优惠券（仅 AVAILABLE 且在有效期内）
     */
    @Update("UPDATE user_coupon SET status = 'LOCKED', lock_order_no = #{orderNo}, "
            + "version = version + 1 WHERE id = #{couponId} AND user_id = #{userId} "
            + "AND status = 'AVAILABLE' AND valid_from <= NOW() "
            + "AND (valid_to IS NULL OR valid_to >= NOW())")
    int lockCoupon(@Param("userId") Long userId,
                   @Param("couponId") Long couponId,
                   @Param("orderNo") String orderNo);

    /**
     * 核销优惠券（仅 LOCKED 且锁定订单匹配）
     */
    @Update("UPDATE user_coupon SET status = 'USED', used_order_no = #{orderNo}, "
            + "used_at = NOW(), lock_order_no = NULL, version = version + 1 "
            + "WHERE id = #{couponId} AND user_id = #{userId} "
            + "AND status = 'LOCKED' AND lock_order_no = #{orderNo}")
    int consumeCoupon(@Param("userId") Long userId,
                      @Param("couponId") Long couponId,
                      @Param("orderNo") String orderNo);

    /**
     * 释放锁定（取消/关单）
     */
    @Update("UPDATE user_coupon SET status = 'AVAILABLE', lock_order_no = NULL, "
            + "version = version + 1 WHERE id = #{couponId} AND user_id = #{userId} "
            + "AND status = 'LOCKED' AND lock_order_no = #{orderNo}")
    int releaseCoupon(@Param("userId") Long userId,
                      @Param("couponId") Long couponId,
                      @Param("orderNo") String orderNo);

    /**
     * 退款恢复（Phase 4 使用；当前仅提供能力）
     */
    @Update("UPDATE user_coupon SET status = 'AVAILABLE', used_order_no = NULL, "
            + "used_at = NULL, lock_order_no = NULL, version = version + 1 "
            + "WHERE id = #{couponId} AND user_id = #{userId} "
            + "AND status = 'USED' AND used_order_no = #{orderNo}")
    int restoreCoupon(@Param("userId") Long userId,
                      @Param("couponId") Long couponId,
                      @Param("orderNo") String orderNo);

}
