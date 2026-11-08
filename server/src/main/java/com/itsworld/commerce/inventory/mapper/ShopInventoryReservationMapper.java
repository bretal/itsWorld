package com.itsworld.commerce.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.inventory.entity.ShopInventoryReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 库存预占数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface ShopInventoryReservationMapper extends BaseMapper<ShopInventoryReservation> {

    @Select("SELECT * FROM shop_inventory_reservation WHERE order_id = #{orderId} AND status = 'RESERVED' ORDER BY id")
    List<ShopInventoryReservation> selectReservedByOrderId(@Param("orderId") Long orderId);

}
