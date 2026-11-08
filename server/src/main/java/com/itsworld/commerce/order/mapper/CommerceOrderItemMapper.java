package com.itsworld.commerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.order.entity.CommerceOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商业订单项数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceOrderItemMapper extends BaseMapper<CommerceOrderItem> {

    @Update("UPDATE commerce_order_item SET fulfilled_qty = #{qty} "
            + "WHERE id = #{itemId} AND fulfilled_qty <= #{qty}")
    int updateFulfilledQty(@Param("itemId") Long itemId, @Param("qty") int qty);

    @Update("UPDATE commerce_order_item SET refunded_amount = refunded_amount + #{amount} "
            + "WHERE id = #{itemId} AND refunded_amount + #{amount} <= payable_total")
    int addRefundedAmount(@Param("itemId") Long itemId, @Param("amount") Long amount);

}
