package com.itsworld.commerce.fulfillment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.fulfillment.entity.CommerceShipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商业发货单数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceShipmentMapper extends BaseMapper<CommerceShipment> {

    @Select("SELECT * FROM commerce_shipment WHERE order_id = #{orderId} ORDER BY id DESC LIMIT 1")
    CommerceShipment selectLatestByOrderId(@Param("orderId") Long orderId);

    @Update("UPDATE commerce_shipment SET status = 'DELIVERED', received_time = NOW(), "
            + "updated_at = NOW() WHERE id = #{shipmentId} AND status = 'SHIPPED'")
    int markDelivered(@Param("shipmentId") Long shipmentId);

    @Select("SELECT * FROM commerce_shipment WHERE status = 'SHIPPED' "
            + "AND shipped_time < #{deadline} ORDER BY id ASC LIMIT 100")
    List<CommerceShipment> selectAutoConfirmable(@Param("deadline") java.time.LocalDateTime deadline);

}
