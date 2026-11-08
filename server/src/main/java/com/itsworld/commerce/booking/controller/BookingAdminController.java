package com.itsworld.commerce.booking.controller;

import com.itsworld.commerce.booking.dto.BookingRescheduleRequest;
import com.itsworld.commerce.booking.dto.BookingSlotSaveRequest;
import com.itsworld.commerce.booking.dto.BookingView;
import com.itsworld.commerce.booking.dto.SlotGenerateRequest;
import com.itsworld.commerce.booking.entity.BookingSlot;
import com.itsworld.commerce.booking.service.BookingService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import com.itsworld.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理端美容预约控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/bookings")
@RequiredArgsConstructor
public class BookingAdminController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    @GetMapping("/slots")
    public Result<PageResult<BookingSlot>> pageSlots(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(bookingService.pageAdminSlots(storeId, date, status,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @PostMapping("/slots")
    public Result<BookingSlot> createSlot(@Valid @RequestBody BookingSlotSaveRequest request) {
        return Result.success(bookingService.createSlot(request), "时段已创建");
    }

    @PutMapping("/slots/{slotId}")
    public Result<BookingSlot> updateSlot(@PathVariable Long slotId,
                                          @Valid @RequestBody BookingSlotSaveRequest request) {
        return Result.success(bookingService.updateSlot(slotId, request), "时段已更新");
    }

    @DeleteMapping("/slots/{slotId}")
    public Result<Void> deleteSlot(@PathVariable Long slotId) {
        bookingService.deleteSlot(slotId);
        return Result.success(null, "时段已删除");
    }

    @PostMapping("/slots/generate")
    public Result<List<BookingSlot>> generateSlots(@Valid @RequestBody SlotGenerateRequest request) {
        return Result.success(bookingService.generateSlots(request), "时段已生成");
    }

    @GetMapping
    public Result<PageResult<BookingView>> pageBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size) {
        return Result.success(bookingService.pageAdminBookings(status, date, storeId, keyword,
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size)));
    }

    @GetMapping("/{bookingNo}")
    public Result<BookingView> getBooking(@PathVariable String bookingNo) {
        return Result.success(bookingService.getAdminBooking(bookingNo));
    }

    @PostMapping("/{bookingNo}/checkin")
    public Result<Void> checkin(@PathVariable String bookingNo) {
        bookingService.checkinBooking(bookingNo, currentUserService.getUserId());
        return Result.success(null, "已签到");
    }

    @PostMapping("/{bookingNo}/start")
    public Result<Void> start(@PathVariable String bookingNo) {
        bookingService.startBooking(bookingNo, currentUserService.getUserId());
        return Result.success(null, "已开始服务");
    }

    @PostMapping("/{bookingNo}/complete")
    public Result<Void> complete(@PathVariable String bookingNo) {
        bookingService.completeBooking(bookingNo, currentUserService.getUserId());
        return Result.success(null, "服务已完成");
    }

    @PostMapping("/{bookingNo}/cancel")
    public Result<Void> cancel(@PathVariable String bookingNo,
                               @RequestParam(required = false) String reason) {
        bookingService.cancelAdminBooking(bookingNo, reason, currentUserService.getUserId());
        return Result.success(null, "预约已取消");
    }

    @PostMapping("/{bookingNo}/reschedule")
    public Result<Void> reschedule(@PathVariable String bookingNo,
                                   @Valid @RequestBody BookingRescheduleRequest request) {
        bookingService.rescheduleAdminBooking(bookingNo, request.getSlotId(),
                currentUserService.getUserId());
        return Result.success(null, "预约已改期");
    }

}
