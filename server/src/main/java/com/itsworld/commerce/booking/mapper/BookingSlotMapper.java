package com.itsworld.commerce.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.booking.entity.BookingSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 可预约时段数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface BookingSlotMapper extends BaseMapper<BookingSlot> {

    @Update("UPDATE booking_slot SET locked_capacity = locked_capacity + #{qty}, "
            + "version = version + 1, updated_at = NOW() "
            + "WHERE id = #{slotId} AND status = 'OPEN' "
            + "AND total_capacity - reserved_capacity - locked_capacity >= #{qty}")
    int lockCapacity(@Param("slotId") Long slotId, @Param("qty") int qty);

    @Update("UPDATE booking_slot SET locked_capacity = locked_capacity - #{qty}, "
            + "reserved_capacity = reserved_capacity + #{qty}, version = version + 1, updated_at = NOW() "
            + "WHERE id = #{slotId} AND locked_capacity >= #{qty}")
    int confirmCapacity(@Param("slotId") Long slotId, @Param("qty") int qty);

    @Update("UPDATE booking_slot SET locked_capacity = locked_capacity - #{qty}, "
            + "version = version + 1, updated_at = NOW() "
            + "WHERE id = #{slotId} AND locked_capacity >= #{qty}")
    int releaseLockedCapacity(@Param("slotId") Long slotId, @Param("qty") int qty);

    @Update("UPDATE booking_slot SET reserved_capacity = reserved_capacity - #{qty}, "
            + "version = version + 1, updated_at = NOW() "
            + "WHERE id = #{slotId} AND reserved_capacity >= #{qty}")
    int releaseReservedCapacity(@Param("slotId") Long slotId, @Param("qty") int qty);

}
