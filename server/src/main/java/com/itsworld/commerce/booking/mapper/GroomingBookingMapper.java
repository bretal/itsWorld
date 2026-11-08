package com.itsworld.commerce.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.booking.entity.GroomingBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 美容预约数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface GroomingBookingMapper extends BaseMapper<GroomingBooking> {

    @Select("SELECT * FROM grooming_booking WHERE booking_no = #{bookingNo} LIMIT 1")
    GroomingBooking selectByBookingNo(@Param("bookingNo") String bookingNo);

    @Select("SELECT * FROM grooming_booking WHERE order_id = #{orderId} LIMIT 1")
    GroomingBooking selectByOrderId(@Param("orderId") Long orderId);

    @Update("UPDATE grooming_booking SET status = 'CONFIRMED', reserve_expire_time = NULL, "
            + "updated_at = NOW() WHERE id = #{bookingId} AND status = 'PENDING_PAYMENT'")
    int confirmIfPendingPayment(@Param("bookingId") Long bookingId);

    @Update("UPDATE grooming_booking SET status = 'CANCELLED', cancel_reason = #{reason}, "
            + "updated_at = NOW() WHERE id = #{bookingId} AND status = 'PENDING_PAYMENT'")
    int cancelIfPendingPayment(@Param("bookingId") Long bookingId, @Param("reason") String reason);

    @Update("UPDATE grooming_booking SET status = 'CANCELLED', cancel_reason = #{reason}, "
            + "updated_at = NOW() WHERE id = #{bookingId} "
            + "AND status IN ('CONFIRMED', 'CHECKED_IN')")
    int cancelIfConfirmed(@Param("bookingId") Long bookingId, @Param("reason") String reason);

    @Update("UPDATE grooming_booking SET status = 'CHECKED_IN', checkin_time = NOW(), "
            + "updated_at = NOW() WHERE id = #{bookingId} AND status = 'CONFIRMED'")
    int checkinIfConfirmed(@Param("bookingId") Long bookingId);

    @Update("UPDATE grooming_booking SET status = 'IN_SERVICE', updated_at = NOW() "
            + "WHERE id = #{bookingId} AND status = 'CHECKED_IN'")
    int startIfCheckedIn(@Param("bookingId") Long bookingId);

    @Update("UPDATE grooming_booking SET status = 'COMPLETED', completed_time = NOW(), "
            + "updated_at = NOW() WHERE id = #{bookingId} AND status = 'IN_SERVICE'")
    int completeIfInService(@Param("bookingId") Long bookingId);

    @Update("UPDATE grooming_booking SET slot_id = #{slotId}, staff_id = #{staffId}, "
            + "updated_at = NOW() WHERE id = #{bookingId} "
            + "AND status IN ('PENDING_PAYMENT', 'CONFIRMED')")
    int rescheduleIfAllowed(@Param("bookingId") Long bookingId,
                            @Param("slotId") Long slotId,
                            @Param("staffId") Long staffId);

}
