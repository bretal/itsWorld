package com.itsworld.commerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.order.entity.CommerceOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 商业订单数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceOrderMapper extends BaseMapper<CommerceOrder> {

    @Select("SELECT * FROM commerce_order WHERE order_no = #{orderNo} LIMIT 1")
    CommerceOrder selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM commerce_order WHERE user_id = #{userId} AND idempotency_key = #{key} LIMIT 1")
    CommerceOrder selectByUserAndIdempotency(@Param("userId") Long userId,
                                             @Param("key") String key);

    @Update("UPDATE commerce_order SET order_status = 'PAID', payment_status = 'PAID', "
            + "paid_amount = #{paidAmount}, paid_time = NOW(), updated_at = NOW() "
            + "WHERE id = #{orderId} AND order_status = 'WAIT_PAY'")
    int markPaid(@Param("orderId") Long orderId, @Param("paidAmount") Long paidAmount);

    @Update("UPDATE commerce_order SET order_status = 'CANCELLED', close_time = NOW(), "
            + "cancel_reason = #{reason}, updated_at = NOW() "
            + "WHERE id = #{orderId} AND order_status = 'WAIT_PAY'")
    int cancelIfWaitPay(@Param("orderId") Long orderId, @Param("reason") String reason);

    @Update("UPDATE commerce_order SET order_status = 'CLOSED', close_time = NOW(), "
            + "cancel_reason = #{reason}, updated_at = NOW() "
            + "WHERE id = #{orderId} AND order_status = 'WAIT_PAY'")
    int closeIfWaitPay(@Param("orderId") Long orderId, @Param("reason") String reason);

    @Update("UPDATE commerce_order SET order_status = 'COMPLETED', "
            + "fulfillment_status = 'COMPLETED', updated_at = NOW() "
            + "WHERE id = #{orderId} AND order_status = 'PAID'")
    int completeIfPaid(@Param("orderId") Long orderId);

    @Update("UPDATE commerce_order SET order_status = 'SHIPPED', "
            + "fulfillment_status = 'SHIPPED', updated_at = NOW() "
            + "WHERE id = #{orderId} AND order_status IN ('PAID', 'PROCESSING')")
    int shipIfPaid(@Param("orderId") Long orderId);

    @Update("UPDATE commerce_order SET order_status = 'COMPLETED', "
            + "fulfillment_status = 'COMPLETED', updated_at = NOW() "
            + "WHERE id = #{orderId} AND order_status = 'SHIPPED'")
    int completeIfShipped(@Param("orderId") Long orderId);

    @Update("UPDATE commerce_order SET refunded_amount = refunded_amount + #{amount}, "
            + "payment_status = #{paymentStatus}, updated_at = NOW() "
            + "WHERE id = #{orderId} AND refunded_amount + #{amount} <= paid_amount")
    int addRefundedAmount(@Param("orderId") Long orderId,
                          @Param("amount") Long amount,
                          @Param("paymentStatus") String paymentStatus);

}
