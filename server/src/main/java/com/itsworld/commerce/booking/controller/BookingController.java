package com.itsworld.commerce.booking.controller;

import com.itsworld.commerce.booking.dto.BookingCreateRequest;
import com.itsworld.commerce.booking.dto.BookingCreateResult;
import com.itsworld.commerce.booking.dto.BookingRescheduleRequest;
import com.itsworld.commerce.booking.dto.BookingSlotView;
import com.itsworld.commerce.booking.dto.BookingView;
import com.itsworld.commerce.booking.dto.PackageBookingOptionView;
import com.itsworld.commerce.booking.service.BookingService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户端美容预约控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/commerce")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    @GetMapping("/booking/slots")
    public Result<List<BookingSlotView>> listSlots(
            @RequestParam Long storeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(bookingService.listAvailableSlots(storeId, date));
    }

    @GetMapping("/booking/package-options")
    public Result<List<PackageBookingOptionView>> listPackageOptions(
            @RequestParam Long serviceId,
            @RequestParam(required = false) Long storeId) {
        Long userId = currentUserService.getUserId();
        return Result.success(bookingService.listPackageOptions(userId, serviceId, storeId));
    }

    @PostMapping("/bookings")
    public Result<BookingCreateResult> createBooking(
            @Valid @RequestBody BookingCreateRequest request) {
        Long userId = currentUserService.getUserId();
        return Result.success(bookingService.createBooking(userId, request), "预约已创建");
    }

    @GetMapping("/bookings")
    public Result<PageResult<BookingView>> listBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        Long userId = currentUserService.getUserId();
        return Result.success(bookingService.pageUserBookings(userId, status,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/bookings/{bookingNo}")
    public Result<BookingView> getBooking(@PathVariable String bookingNo) {
        Long userId = currentUserService.getUserId();
        return Result.success(bookingService.getUserBooking(userId, bookingNo));
    }

    @PostMapping("/bookings/{bookingNo}/cancel")
    public Result<Void> cancelBooking(@PathVariable String bookingNo,
                                      @RequestParam(required = false) String reason) {
        Long userId = currentUserService.getUserId();
        bookingService.cancelUserBooking(userId, bookingNo, reason);
        return Result.success(null, "预约已取消");
    }

    @PostMapping("/bookings/{bookingNo}/reschedule")
    public Result<Void> rescheduleBooking(@PathVariable String bookingNo,
                                          @Valid @RequestBody BookingRescheduleRequest request) {
        Long userId = currentUserService.getUserId();
        bookingService.rescheduleUserBooking(userId, bookingNo, request.getSlotId());
        return Result.success(null, "预约已改期");
    }

}
