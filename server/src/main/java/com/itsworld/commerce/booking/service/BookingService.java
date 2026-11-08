package com.itsworld.commerce.booking.service;

import com.itsworld.commerce.booking.dto.BookingCreateRequest;
import com.itsworld.commerce.booking.dto.BookingCreateResult;
import com.itsworld.commerce.booking.dto.BookingSlotSaveRequest;
import com.itsworld.commerce.booking.dto.BookingSlotView;
import com.itsworld.commerce.booking.dto.BookingView;
import com.itsworld.commerce.booking.dto.PackageBookingOptionView;
import com.itsworld.commerce.booking.dto.SlotGenerateRequest;
import com.itsworld.commerce.booking.entity.BookingSlot;
import com.itsworld.common.result.PageResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 美容预约业务接口
 * 容量锁定/确认/释放、预约创建/改期/取消、签到/完成，以及管理端时段维护。
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface BookingService {

    // ---------- 用户端 ----------

    /**
     * 指定门店/日期的可预约时段
     */
    List<BookingSlotView> listAvailableSlots(Long storeId, LocalDate serviceDate);

    /**
     * 指定服务下当前用户可用的套餐次数项
     */
    List<PackageBookingOptionView> listPackageOptions(Long userId, Long serviceId, Long storeId);

    /**
     * 创建预约：在线全额预付或套餐次数核销
     */
    BookingCreateResult createBooking(Long userId, BookingCreateRequest request);

    PageResult<BookingView> pageUserBookings(Long userId, String status, long pageNum, long pageSize);

    BookingView getUserBooking(Long userId, String bookingNo);

    /**
     * 用户取消：仅未支付预约；套餐预约/已支付预约需门店处理
     */
    void cancelUserBooking(Long userId, String bookingNo, String reason);

    /**
     * 用户改期：未支付/已确认预约
     */
    void rescheduleUserBooking(Long userId, String bookingNo, Long slotId);

    // ---------- 管理端：时段 ----------

    PageResult<BookingSlot> pageAdminSlots(Long storeId, LocalDate serviceDate, String status,
                                           long pageNum, long pageSize);

    BookingSlot createSlot(BookingSlotSaveRequest request);

    BookingSlot updateSlot(Long slotId, BookingSlotSaveRequest request);

    void deleteSlot(Long slotId);

    /**
     * 按区间批量生成时段，已存在的时段跳过
     */
    List<BookingSlot> generateSlots(SlotGenerateRequest request);

    // ---------- 管理端：预约 ----------

    PageResult<BookingView> pageAdminBookings(String status, LocalDate serviceDate, Long storeId,
                                              String keyword, long pageNum, long pageSize);

    BookingView getAdminBooking(String bookingNo);

    void checkinBooking(String bookingNo, Long operatorId);

    void startBooking(String bookingNo, Long operatorId);

    void completeBooking(String bookingNo, Long operatorId);

    void cancelAdminBooking(String bookingNo, String reason, Long operatorId);

    void rescheduleAdminBooking(String bookingNo, Long slotId, Long operatorId);

}
