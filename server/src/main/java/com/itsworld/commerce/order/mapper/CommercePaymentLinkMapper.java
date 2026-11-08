package com.itsworld.commerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.order.entity.CommercePaymentLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商业支付记录数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommercePaymentLinkMapper extends BaseMapper<CommercePaymentLink> {

    @Select("SELECT * FROM commerce_payment_link WHERE payment_order_id = #{paymentOrderId} LIMIT 1")
    CommercePaymentLink selectByPaymentOrderId(@Param("paymentOrderId") String paymentOrderId);

    @Select("SELECT * FROM commerce_payment_link WHERE idempotency_key = #{key} LIMIT 1")
    CommercePaymentLink selectByIdempotencyKey(@Param("key") String key);

    @Select("SELECT * FROM commerce_payment_link WHERE order_id = #{orderId} ORDER BY id DESC LIMIT 1")
    CommercePaymentLink selectLatestByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM commerce_payment_link WHERE order_id = #{orderId} "
            + "AND status = 'PAID' ORDER BY id DESC LIMIT 1")
    CommercePaymentLink selectLatestPaidByOrderId(@Param("orderId") Long orderId);

}
