package com.itsworld.commerce.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.commerce.booking.dto.BookingCreateRequest;
import com.itsworld.commerce.booking.dto.BookingCreateResult;
import com.itsworld.commerce.booking.dto.BookingSlotSaveRequest;
import com.itsworld.commerce.booking.dto.BookingSlotView;
import com.itsworld.commerce.booking.dto.BookingView;
import com.itsworld.commerce.booking.dto.PackageBookingOptionView;
import com.itsworld.commerce.booking.dto.SlotGenerateRequest;
import com.itsworld.commerce.booking.entity.BookingSlot;
import com.itsworld.commerce.booking.entity.GroomingBooking;
import com.itsworld.commerce.booking.entity.GroomingBookingLog;
import com.itsworld.commerce.booking.mapper.BookingSlotMapper;
import com.itsworld.commerce.booking.mapper.GroomingBookingLogMapper;
import com.itsworld.commerce.booking.mapper.GroomingBookingMapper;
import com.itsworld.commerce.booking.service.BookingService;
import com.itsworld.commerce.common.CommercePriceUtils;
import com.itsworld.commerce.grooming.entity.GroomingService;
import com.itsworld.commerce.grooming.mapper.GroomingServiceMapper;
import com.itsworld.commerce.grooming.service.GroomingCatalogService;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.entity.CommerceOrderDiscount;
import com.itsworld.commerce.order.entity.CommerceOrderItem;
import com.itsworld.commerce.order.entity.CommerceOrderStatusLog;
import com.itsworld.commerce.order.event.CommerceOrderStatusChangedEvent;
import com.itsworld.commerce.order.mapper.CommerceOrderDiscountMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderItemMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderStatusLogMapper;
import com.itsworld.commerce.order.service.CommerceOrderService;
import com.itsworld.commerce.packagebiz.service.PackageService;
import com.itsworld.commerce.store.entity.CommerceStaff;
import com.itsworld.commerce.store.entity.CommerceStore;
import com.itsworld.commerce.store.mapper.CommerceStaffMapper;
import com.itsworld.commerce.store.mapper.CommerceStoreMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.member.dto.CouponQuote;
import com.itsworld.member.service.CouponService;
import com.itsworld.member.service.MemberService;
import com.itsworld.pet.entity.Pet;
import com.itsworld.pet.service.PetService;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 美容预约业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final String PAY_TYPE_ONLINE = "ONLINE";
    private static final String PAY_TYPE_PACKAGE = "PACKAGE";
    private final com.itsworld.member.service.MemberTaskService memberTaskService;

    private static final String ORDER_TYPE_GROOMING = "GROOMING";
    private static final String SLOT_OPEN = "OPEN";
    private static final String SLOT_CLOSED = "CLOSED";
    private static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_IN_SERVICE = "IN_SERVICE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final int PAY_EXPIRE_MINUTES = 15;

    private final BookingSlotMapper slotMapper;
    private final GroomingBookingMapper bookingMapper;
    private final GroomingBookingLogMapper bookingLogMapper;
    private final GroomingCatalogService groomingCatalogService;
    private final GroomingServiceMapper groomingServiceMapper;
    private final PackageService packageService;
    private final PetService petService;
    private final UserService userService;
    private final CommerceStoreMapper storeMapper;
    private final CommerceStaffMapper staffMapper;
    private final MemberService memberService;
    private final CouponService couponService;
    private final CommerceOrderService orderService;
    private final CommerceOrderMapper orderMapper;
    private final CommerceOrderItemMapper orderItemMapper;
    private final CommerceOrderDiscountMapper orderDiscountMapper;
    private final CommerceOrderStatusLogMapper orderStatusLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<BookingSlotView> listAvailableSlots(Long storeId, LocalDate serviceDate) {
        LocalDate date = serviceDate != null ? serviceDate : LocalDate.now();
        List<BookingSlot> slots = slotMapper.selectList(new LambdaQueryWrapper<BookingSlot>()
                .eq(BookingSlot::getStoreId, storeId)
                .eq(BookingSlot::getServiceDate, date)
                .eq(BookingSlot::getStatus, SLOT_OPEN)
                .orderByAsc(BookingSlot::getStartTime)
                .orderByAsc(BookingSlot::getId));
        LocalDateTime now = LocalDateTime.now();
        List<BookingSlotView> views = new ArrayList<>();
        for (BookingSlot slot : slots) {
            int available = availableCapacity(slot);
            if (available <= 0) {
                continue;
            }
            if (date.equals(LocalDate.now()) && !slot.getEndTime().isAfter(now.toLocalTime())) {
                continue;
            }
            views.add(toSlotView(slot, available));
        }
        return views;
    }

    @Override
    public List<PackageBookingOptionView> listPackageOptions(Long userId, Long serviceId,
                                                             Long storeId) {
        return packageService.listBookingOptions(userId, serviceId, storeId);
    }

    @Override
    @Transactional
    public BookingCreateResult createBooking(Long userId, BookingCreateRequest request) {
        String payType = normalizePayType(request.getPayType());
        CommerceStore store = requireOpenStore(request.getStoreId());
        GroomingService service = groomingCatalogService.requireEnabledService(request.getServiceId());
        groomingCatalogService.requireEnabledServiceStore(request.getServiceId(), request.getStoreId());
        groomingCatalogService.validatePetEligible(request.getServiceId(), userId, request.getPetId());
        BookingSlot slot = requireBookableSlot(request.getSlotId(), store.getId());

        if (PAY_TYPE_ONLINE.equals(payType)) {
            String idempotencyKey = StringUtils.hasText(request.getIdempotencyKey())
                    ? request.getIdempotencyKey().trim() : UUID.randomUUID().toString();
            CommerceOrder existing = orderMapper.selectByUserAndIdempotency(userId, idempotencyKey);
            if (existing != null) {
                GroomingBooking existedBooking = bookingMapper.selectByOrderId(existing.getId());
                if (existedBooking == null) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(),
                            "订单已存在但预约缺失，请刷新后重试");
                }
                return toCreateResult(existedBooking, existing);
            }
        }

        if (slotMapper.lockCapacity(slot.getId(), 1) != 1) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_FULL);
        }

        String bookingNo = generateBookingNo();
        LocalDateTime now = LocalDateTime.now();
        if (PAY_TYPE_PACKAGE.equals(payType)) {
            return createPackageBooking(userId, request, store, service, slot, bookingNo, now);
        }
        return createOnlineBooking(userId, request, store, service, slot, bookingNo, now);
    }

    private BookingCreateResult createOnlineBooking(Long userId, BookingCreateRequest request,
                                                    CommerceStore store, GroomingService service,
                                                    BookingSlot slot, String bookingNo,
                                                    LocalDateTime now) {
        long storePrice = groomingCatalogService.resolveStorePrice(
                service.getId(), store.getId());
        BigDecimal discountRate = memberService.getCurrentDiscountRate(userId);
        long memberPrice = CommercePriceUtils.applyDiscount(storePrice, discountRate);
        long memberDiscount = storePrice - memberPrice;
        CouponQuote couponQuote = request.getCouponId() != null
                ? couponService.quoteCoupon(userId, request.getCouponId(), memberPrice) : null;
        long couponDiscount = couponQuote != null ? couponQuote.getDiscountAmount() : 0L;
        long payableAmount = Math.max(0L, memberPrice - couponDiscount);
        if (payableAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "预约应付金额必须大于0");
        }

        LocalDateTime expireTime = now.plusMinutes(PAY_EXPIRE_MINUTES);
        String orderNo = generateOrderNo();
        String idempotencyKey = StringUtils.hasText(request.getIdempotencyKey())
                ? request.getIdempotencyKey().trim() : UUID.randomUUID().toString();

        CommerceOrder order = CommerceOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .orderType(ORDER_TYPE_GROOMING)
                .orderStatus("WAIT_PAY")
                .paymentStatus("UNPAID")
                .fulfillmentStatus("PENDING")
                .originalAmount(storePrice)
                .discountAmount(memberDiscount + couponDiscount)
                .payableAmount(payableAmount)
                .paidAmount(0L)
                .refundedAmount(0L)
                .couponId(request.getCouponId())
                .storeSnapshot(toStoreSnapshot(store))
                .expireTime(expireTime)
                .remark("美容预约：" + bookingNo)
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
        orderMapper.insert(order);

        CommerceOrderItem orderItem = CommerceOrderItem.builder()
                .orderId(order.getId())
                .itemType(ORDER_TYPE_GROOMING)
                .targetId(service.getId())
                .petId(request.getPetId())
                .quantity(1)
                .titleSnapshot(service.getName())
                .specSnapshot(toJson(buildBookingSpecSnapshot(
                        bookingNo, service, store, slot, request.getPetId())))
                .unitPrice(storePrice)
                .originalTotal(storePrice)
                .discountAllocated(memberDiscount + couponDiscount)
                .payableTotal(payableAmount)
                .fulfilledQty(0)
                .refundedAmount(0L)
                .createdAt(now)
                .build();
        orderItemMapper.insert(orderItem);

        if (memberDiscount > 0) {
            orderDiscountMapper.insert(CommerceOrderDiscount.builder()
                    .orderId(order.getId())
                    .orderItemId(orderItem.getId())
                    .discountType("MEMBER")
                    .discountAmount(memberDiscount)
                    .ruleSnapshot(toJson(Map.of("discountRate", discountRate)))
                    .createdAt(now)
                    .build());
        }
        if (request.getCouponId() != null) {
            couponService.lockCoupon(userId, request.getCouponId(), orderNo);
            if (couponDiscount > 0) {
                orderDiscountMapper.insert(CommerceOrderDiscount.builder()
                        .orderId(order.getId())
                        .orderItemId(null)
                        .discountType("COUPON")
                        .sourceId(request.getCouponId())
                        .discountAmount(couponDiscount)
                        .ruleSnapshot(toJson(Map.of("couponId", request.getCouponId())))
                        .createdAt(now)
                        .build());
            }
        }

        GroomingBooking booking = GroomingBooking.builder()
                .bookingNo(bookingNo)
                .userId(userId)
                .petId(request.getPetId())
                .storeId(store.getId())
                .staffId(resolveStaffId(slot))
                .slotId(slot.getId())
                .orderId(order.getId())
                .serviceId(service.getId())
                .status(STATUS_PENDING_PAYMENT)
                .reserveExpireTime(expireTime)
                .createdAt(now)
                .updatedAt(now)
                .build();
        bookingMapper.insert(booking);

        writeBookingLog(booking.getId(), null, STATUS_PENDING_PAYMENT, "CREATE",
                "USER", userId, "创建预约并生成待支付订单");
        orderStatusLogMapper.insert(CommerceOrderStatusLog.builder()
                .orderId(order.getId())
                .fromStatus(null)
                .toStatus("WAIT_PAY")
                .action("CREATE")
                .operatorType("USER")
                .operatorId(userId)
                .remark("提交美容预约")
                .createdAt(now)
                .build());

        log.info("预约创建: bookingNo={}, orderNo={}, userId={}, payable={}",
                bookingNo, orderNo, userId, payableAmount);
        return toCreateResult(booking, order);
    }

    private BookingCreateResult createPackageBooking(Long userId, BookingCreateRequest request,
                                                     CommerceStore store, GroomingService service,
                                                     BookingSlot slot, String bookingNo,
                                                     LocalDateTime now) {
        if (request.getUserPackageItemId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "请选择套餐次数");
        }
        GroomingBooking booking = GroomingBooking.builder()
                .bookingNo(bookingNo)
                .userId(userId)
                .petId(request.getPetId())
                .storeId(store.getId())
                .staffId(resolveStaffId(slot))
                .slotId(slot.getId())
                .serviceId(service.getId())
                .status(STATUS_CONFIRMED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        bookingMapper.insert(booking);
        if (slotMapper.confirmCapacity(slot.getId(), 1) != 1) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_FULL);
        }
        packageService.freezeForBooking(userId, request.getUserPackageItemId(),
                booking.getId(), service.getId(), store.getId(), request.getPetId());

        writeBookingLog(booking.getId(), null, STATUS_CONFIRMED, "CREATE_PACKAGE",
                "USER", userId, "套餐次数预约");
        log.info("套餐预约创建: bookingNo={}, userId={}, packageItemId={}",
                bookingNo, userId, request.getUserPackageItemId());
        return BookingCreateResult.builder()
                .bookingId(booking.getId())
                .bookingNo(bookingNo)
                .status(STATUS_CONFIRMED)
                .payableAmount(0L)
                .build();
    }

    @Override
    public PageResult<BookingView> pageUserBookings(Long userId, String status,
                                                    long pageNum, long pageSize) {
        Page<GroomingBooking> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GroomingBooking> wrapper = new LambdaQueryWrapper<GroomingBooking>()
                .eq(GroomingBooking::getUserId, userId)
                .orderByDesc(GroomingBooking::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(GroomingBooking::getStatus, status.trim().toUpperCase());
        }
        Page<GroomingBooking> result = bookingMapper.selectPage(page, wrapper);
        List<BookingView> records = new ArrayList<>();
        for (GroomingBooking booking : result.getRecords()) {
            records.add(buildBookingView(booking, false));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public BookingView getUserBooking(Long userId, String bookingNo) {
        GroomingBooking booking = requireOwnedBooking(userId, bookingNo);
        return buildBookingView(booking, true);
    }

    @Override
    @Transactional
    public void cancelUserBooking(Long userId, String bookingNo, String reason) {
        GroomingBooking booking = requireOwnedBooking(userId, bookingNo);
        if (booking.getOrderId() == null) {
            throw new BusinessException(ErrorCode.BOOKING_STATUS_ERROR.getCode(),
                    "套餐预约请联系门店取消");
        }
        if (!STATUS_PENDING_PAYMENT.equals(booking.getStatus())) {
            throw new BusinessException(ErrorCode.BOOKING_STATUS_ERROR.getCode(),
                    "已支付预约请联系门店处理退款");
        }
        CommerceOrder order = orderMapper.selectById(booking.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "预约订单不存在");
        }
        String finalReason = StringUtils.hasText(reason) ? reason.trim() : "用户取消预约";
        orderService.cancelUserOrder(userId, order.getOrderNo(), finalReason);
    }

    @Override
    @Transactional
    public void rescheduleUserBooking(Long userId, String bookingNo, Long slotId) {
        GroomingBooking booking = requireOwnedBooking(userId, bookingNo);
        doReschedule(booking, slotId, "USER", userId);
    }

    @Override
    public PageResult<BookingSlot> pageAdminSlots(Long storeId, LocalDate serviceDate, String status,
                                                  long pageNum, long pageSize) {
        Page<BookingSlot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BookingSlot> wrapper = new LambdaQueryWrapper<BookingSlot>()
                .orderByDesc(BookingSlot::getServiceDate)
                .orderByAsc(BookingSlot::getStartTime)
                .orderByAsc(BookingSlot::getId);
        if (storeId != null) {
            wrapper.eq(BookingSlot::getStoreId, storeId);
        }
        if (serviceDate != null) {
            wrapper.eq(BookingSlot::getServiceDate, serviceDate);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BookingSlot::getStatus, normalizeSlotStatus(status));
        }
        Page<BookingSlot> result = slotMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public BookingSlot createSlot(BookingSlotSaveRequest request) {
        CommerceStore store = requireOpenStore(request.getStoreId());
        Long staffId = request.getStaffId() != null ? request.getStaffId() : 0L;
        validateStaff(store.getId(), staffId);
        validateSlotTimes(request.getStartTime(), request.getEndTime());
        validateSlotDate(request.getServiceDate());
        try {
            BookingSlot slot = BookingSlot.builder()
                    .storeId(store.getId())
                    .staffId(staffId)
                    .serviceDate(request.getServiceDate())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .totalCapacity(request.getTotalCapacity())
                    .reservedCapacity(0)
                    .lockedCapacity(0)
                    .status(normalizeSlotStatus(request.getStatus()))
                    .version(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            slotMapper.insert(slot);
            return slot;
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "该门店在此时段已有配置");
        }
    }

    @Override
    @Transactional
    public BookingSlot updateSlot(Long slotId, BookingSlotSaveRequest request) {
        BookingSlot slot = requireSlot(slotId);
        CommerceStore store = requireOpenStore(request.getStoreId());
        Long staffId = request.getStaffId() != null ? request.getStaffId() : 0L;
        validateStaff(store.getId(), staffId);
        validateSlotTimes(request.getStartTime(), request.getEndTime());
        validateSlotDate(request.getServiceDate());
        int used = safeInt(slot.getReservedCapacity()) + safeInt(slot.getLockedCapacity());
        if (request.getTotalCapacity() < used) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(),
                    "容量不能小于已预约/锁定数量 " + used);
        }
        Long bookingCount = bookingMapper.selectCount(new LambdaQueryWrapper<GroomingBooking>()
                .eq(GroomingBooking::getSlotId, slotId));
        boolean resourceChanged = !slot.getStoreId().equals(store.getId())
                || !slot.getServiceDate().equals(request.getServiceDate())
                || !slot.getStartTime().equals(request.getStartTime())
                || !slot.getEndTime().equals(request.getEndTime())
                || !slot.getStaffId().equals(staffId);
        if (bookingCount != null && bookingCount > 0 && resourceChanged) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(),
                    "时段已有预约记录，不能修改门店/日期/时间/员工");
        }
        slot.setStoreId(store.getId());
        slot.setStaffId(staffId);
        slot.setServiceDate(request.getServiceDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setTotalCapacity(request.getTotalCapacity());
        slot.setStatus(normalizeSlotStatus(request.getStatus()));
        slot.setVersion(safeInt(slot.getVersion()) + 1);
        slot.setUpdatedAt(LocalDateTime.now());
        try {
            slotMapper.updateById(slot);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "该门店在此时段已有配置");
        }
        return slot;
    }

    @Override
    @Transactional
    public void deleteSlot(Long slotId) {
        BookingSlot slot = requireSlot(slotId);
        int used = safeInt(slot.getReservedCapacity()) + safeInt(slot.getLockedCapacity());
        if (used > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "时段已被预约，不能删除");
        }
        Long bookingCount = bookingMapper.selectCount(new LambdaQueryWrapper<GroomingBooking>()
                .eq(GroomingBooking::getSlotId, slotId));
        if (bookingCount != null && bookingCount > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "时段存在预约记录，不能删除");
        }
        slotMapper.deleteById(slotId);
    }

    @Override
    @Transactional
    public List<BookingSlot> generateSlots(SlotGenerateRequest request) {
        CommerceStore store = requireOpenStore(request.getStoreId());
        Long staffId = request.getStaffId() != null ? request.getStaffId() : 0L;
        validateStaff(store.getId(), staffId);
        validateSlotTimes(request.getStartTime(), request.getEndTime());
        if (request.getIntervalMinutes() == null || request.getIntervalMinutes() < 15) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "时段间隔至少15分钟");
        }
        validateSlotDate(request.getServiceDate());
        List<BookingSlot> created = new ArrayList<>();
        LocalTime cursor = request.getStartTime();
        while (cursor.isBefore(request.getEndTime())) {
            LocalTime end = cursor.plusMinutes(request.getIntervalMinutes());
            if (end.isAfter(request.getEndTime())) {
                end = request.getEndTime();
            }
            Long exists = slotMapper.selectCount(new LambdaQueryWrapper<BookingSlot>()
                    .eq(BookingSlot::getStoreId, store.getId())
                    .eq(BookingSlot::getStaffId, staffId)
                    .eq(BookingSlot::getServiceDate, request.getServiceDate())
                    .eq(BookingSlot::getStartTime, cursor)
                    .eq(BookingSlot::getEndTime, end));
            if (exists == null || exists == 0) {
                BookingSlot slot = BookingSlot.builder()
                        .storeId(store.getId())
                        .staffId(staffId)
                        .serviceDate(request.getServiceDate())
                        .startTime(cursor)
                        .endTime(end)
                        .totalCapacity(request.getTotalCapacity())
                        .reservedCapacity(0)
                        .lockedCapacity(0)
                        .status(SLOT_OPEN)
                        .version(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                slotMapper.insert(slot);
                created.add(slot);
            }
            cursor = end;
        }
        return created;
    }

    @Override
    public PageResult<BookingView> pageAdminBookings(String status, LocalDate serviceDate,
                                                     Long storeId, String keyword,
                                                     long pageNum, long pageSize) {
        LambdaQueryWrapper<BookingSlot> slotWrapper = new LambdaQueryWrapper<>();
        if (serviceDate != null) {
            slotWrapper.eq(BookingSlot::getServiceDate, serviceDate);
        }
        if (storeId != null) {
            slotWrapper.eq(BookingSlot::getStoreId, storeId);
        }
        List<Long> slotIds = null;
        if (serviceDate != null || storeId != null) {
            List<BookingSlot> slots = slotMapper.selectList(slotWrapper);
            slotIds = slots.stream().map(BookingSlot::getId).toList();
            if (slotIds.isEmpty()) {
                return PageResult.of(List.of(), 0L, pageNum, pageSize);
            }
        }

        Page<GroomingBooking> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GroomingBooking> wrapper = new LambdaQueryWrapper<GroomingBooking>()
                .orderByDesc(GroomingBooking::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(GroomingBooking::getStatus, status.trim().toUpperCase());
        }
        if (slotIds != null) {
            wrapper.in(GroomingBooking::getSlotId, slotIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(GroomingBooking::getBookingNo, keyword.trim());
        }
        Page<GroomingBooking> result = bookingMapper.selectPage(page, wrapper);
        List<BookingView> records = new ArrayList<>();
        for (GroomingBooking booking : result.getRecords()) {
            records.add(buildBookingView(booking, false));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public BookingView getAdminBooking(String bookingNo) {
        GroomingBooking booking = bookingMapper.selectByBookingNo(bookingNo);
        if (booking == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }
        return buildBookingView(booking, true);
    }

    @Override
    @Transactional
    public void checkinBooking(String bookingNo, Long operatorId) {
        GroomingBooking booking = requireBooking(bookingNo);
        if (bookingMapper.checkinIfConfirmed(booking.getId()) != 1) {
            throw statusError(booking);
        }
        writeBookingLog(booking.getId(), STATUS_CONFIRMED, STATUS_CHECKED_IN, "CHECKIN",
                "ADMIN", operatorId, "门店签到");
    }

    @Override
    @Transactional
    public void startBooking(String bookingNo, Long operatorId) {
        GroomingBooking booking = requireBooking(bookingNo);
        if (bookingMapper.startIfCheckedIn(booking.getId()) != 1) {
            throw statusError(booking);
        }
        writeBookingLog(booking.getId(), STATUS_CHECKED_IN, STATUS_IN_SERVICE, "START",
                "ADMIN", operatorId, "开始服务");
    }

    @Override
    @Transactional
    public void completeBooking(String bookingNo, Long operatorId) {
        GroomingBooking booking = requireBooking(bookingNo);
        if (bookingMapper.completeIfInService(booking.getId()) != 1) {
            throw statusError(booking);
        }
        packageService.consumeForBooking(booking.getId(), operatorId);
        memberTaskService.recordProgress(booking.getUserId(), "GROOMING_COMPLETE");
        if (booking.getOrderId() != null) {
            orderMapper.completeIfPaid(booking.getOrderId());
        }
        writeBookingLog(booking.getId(), STATUS_IN_SERVICE, STATUS_COMPLETED, "COMPLETE",
                "ADMIN", operatorId, "服务完成");
    }

    @Override
    @Transactional
    public void cancelAdminBooking(String bookingNo, String reason, Long operatorId) {
        GroomingBooking booking = requireBooking(bookingNo);
        String finalReason = StringUtils.hasText(reason) ? reason.trim() : "门店取消预约";
        if (STATUS_PENDING_PAYMENT.equals(booking.getStatus())) {
            if (booking.getOrderId() != null) {
                CommerceOrder order = orderMapper.selectById(booking.getOrderId());
                if (order == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "预约订单不存在");
                }
                orderService.closeAdminOrder(order.getOrderNo(), finalReason);
                return;
            }
            if (bookingMapper.cancelIfPendingPayment(booking.getId(), finalReason) != 1) {
                throw statusError(booking);
            }
            slotMapper.releaseLockedCapacity(booking.getSlotId(), 1);
            writeBookingLog(booking.getId(), STATUS_PENDING_PAYMENT, STATUS_CANCELLED,
                    "CANCEL", "ADMIN", operatorId, finalReason);
            return;
        }
        if (STATUS_CONFIRMED.equals(booking.getStatus())
                || STATUS_CHECKED_IN.equals(booking.getStatus())) {
            String fromStatus = booking.getStatus();
            if (bookingMapper.cancelIfConfirmed(booking.getId(), finalReason) != 1) {
                throw statusError(booking);
            }
            slotMapper.releaseReservedCapacity(booking.getSlotId(), 1);
            packageService.releaseForBooking(booking.getId());
            String remark = booking.getOrderId() != null
                    ? finalReason + "；已支付订单退款待售后处理" : finalReason;
            writeBookingLog(booking.getId(), fromStatus, STATUS_CANCELLED, "CANCEL",
                    "ADMIN", operatorId, remark);
            return;
        }
        throw new BusinessException(ErrorCode.BOOKING_STATUS_ERROR.getCode(),
                "当前预约状态不可取消");
    }

    @Override
    @Transactional
    public void rescheduleAdminBooking(String bookingNo, Long slotId, Long operatorId) {
        GroomingBooking booking = requireBooking(bookingNo);
        doReschedule(booking, slotId, "ADMIN", operatorId);
    }

    @EventListener
    public void onOrderStatusChanged(CommerceOrderStatusChangedEvent event) {
        CommerceOrder order = event.getOrder();
        if (!ORDER_TYPE_GROOMING.equals(order.getOrderType())) {
            return;
        }
        GroomingBooking booking = bookingMapper.selectByOrderId(order.getId());
        if (booking == null) {
            return;
        }
        if (CommerceOrderStatusChangedEvent.ACTION_PAID.equals(event.getAction())) {
            if (bookingMapper.confirmIfPendingPayment(booking.getId()) == 1) {
                if (slotMapper.confirmCapacity(booking.getSlotId(), 1) != 1) {
                    throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "预约容量确认失败");
                }
                writeBookingLog(booking.getId(), STATUS_PENDING_PAYMENT, STATUS_CONFIRMED,
                        "PAY", event.getOperatorType(), event.getOperatorId(), "支付成功，预约确认");
            }
            return;
        }
        if (CommerceOrderStatusChangedEvent.ACTION_CANCELLED.equals(event.getAction())
                || CommerceOrderStatusChangedEvent.ACTION_CLOSED.equals(event.getAction())) {
            if (bookingMapper.cancelIfPendingPayment(booking.getId(), "订单取消/关闭") == 1) {
                slotMapper.releaseLockedCapacity(booking.getSlotId(), 1);
                writeBookingLog(booking.getId(), STATUS_PENDING_PAYMENT, STATUS_CANCELLED,
                        CommerceOrderStatusChangedEvent.ACTION_CLOSED.equals(event.getAction())
                                ? "CLOSE" : "CANCEL",
                        event.getOperatorType(), event.getOperatorId(), "订单取消/关闭，预约释放");
            }
        }
    }

    private void doReschedule(GroomingBooking booking, Long newSlotId,
                              String operatorType, Long operatorId) {
        String status = booking.getStatus();
        if (!STATUS_PENDING_PAYMENT.equals(status) && !STATUS_CONFIRMED.equals(status)) {
            throw new BusinessException(ErrorCode.BOOKING_STATUS_ERROR.getCode(),
                    "当前预约状态不可改期");
        }
        BookingSlot newSlot = requireBookableSlot(newSlotId, booking.getStoreId());
        if (newSlot.getId().equals(booking.getSlotId())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "新时段与当前时段相同");
        }
        if (slotMapper.lockCapacity(newSlot.getId(), 1) != 1) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_FULL);
        }
        if (STATUS_CONFIRMED.equals(status)
                && slotMapper.confirmCapacity(newSlot.getId(), 1) != 1) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "新时段容量确认失败");
        }
        if (STATUS_PENDING_PAYMENT.equals(status)) {
            slotMapper.releaseLockedCapacity(booking.getSlotId(), 1);
        } else {
            slotMapper.releaseReservedCapacity(booking.getSlotId(), 1);
        }
        Long oldSlotId = booking.getSlotId();
        if (bookingMapper.rescheduleIfAllowed(booking.getId(), newSlot.getId(),
                resolveStaffId(newSlot)) != 1) {
            throw statusError(booking);
        }
        writeBookingLog(booking.getId(), status, status, "RESCHEDULE", operatorType, operatorId,
                "时段 " + oldSlotId + " 调整为 " + newSlot.getId());
    }

    private BookingView buildBookingView(GroomingBooking booking, boolean withLogs) {
        BookingSlot slot = slotMapper.selectById(booking.getSlotId());
        GroomingService service = groomingServiceMapper.selectById(booking.getServiceId());
        CommerceStore store = storeMapper.selectById(booking.getStoreId());
        Pet pet = petService.getPetById(booking.getPetId());
        User user = userService.getUserById(booking.getUserId());
        CommerceOrder order = booking.getOrderId() != null
                ? orderMapper.selectById(booking.getOrderId()) : null;
        CommerceStaff staff = booking.getStaffId() != null
                ? staffMapper.selectById(booking.getStaffId()) : null;
        List<GroomingBookingLog> logs = null;
        if (withLogs) {
            logs = bookingLogMapper.selectList(new LambdaQueryWrapper<GroomingBookingLog>()
                    .eq(GroomingBookingLog::getBookingId, booking.getId())
                    .orderByAsc(GroomingBookingLog::getId));
        }
        return BookingView.builder()
                .id(booking.getId())
                .bookingNo(booking.getBookingNo())
                .userId(booking.getUserId())
                .username(user != null ? user.getUsername() : null)
                .petId(booking.getPetId())
                .petName(pet != null ? pet.getPetName() : null)
                .storeId(booking.getStoreId())
                .storeName(store != null ? store.getName() : null)
                .staffId(booking.getStaffId())
                .slotId(booking.getSlotId())
                .serviceDate(slot != null ? slot.getServiceDate() : null)
                .startTime(slot != null ? slot.getStartTime() : null)
                .endTime(slot != null ? slot.getEndTime() : null)
                .orderId(booking.getOrderId())
                .orderNo(order != null ? order.getOrderNo() : null)
                .serviceId(booking.getServiceId())
                .serviceName(service != null ? service.getName() : null)
                .status(booking.getStatus())
                .reserveExpireTime(booking.getReserveExpireTime())
                .checkinTime(booking.getCheckinTime())
                .completedTime(booking.getCompletedTime())
                .cancelReason(booking.getCancelReason())
                .createdAt(booking.getCreatedAt())
                .logs(logs)
                .build();
    }

    private BookingSlotView toSlotView(BookingSlot slot, int available) {
        return BookingSlotView.builder()
                .id(slot.getId())
                .storeId(slot.getStoreId())
                .staffId(slot.getStaffId())
                .serviceDate(slot.getServiceDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .totalCapacity(slot.getTotalCapacity())
                .reservedCapacity(slot.getReservedCapacity())
                .lockedCapacity(slot.getLockedCapacity())
                .availableCapacity(available)
                .status(slot.getStatus())
                .build();
    }

    private BookingCreateResult toCreateResult(GroomingBooking booking, CommerceOrder order) {
        return BookingCreateResult.builder()
                .bookingId(booking.getId())
                .bookingNo(booking.getBookingNo())
                .status(booking.getStatus())
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .payableAmount(order.getPayableAmount())
                .expireTime(order.getExpireTime())
                .build();
    }

    private GroomingBooking requireOwnedBooking(Long userId, String bookingNo) {
        GroomingBooking booking = requireBooking(bookingNo);
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }
        return booking;
    }

    private GroomingBooking requireBooking(String bookingNo) {
        GroomingBooking booking = bookingMapper.selectByBookingNo(bookingNo);
        if (booking == null) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }
        return booking;
    }

    private BookingSlot requireSlot(Long slotId) {
        BookingSlot slot = slotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_NOT_FOUND);
        }
        return slot;
    }

    private BookingSlot requireBookableSlot(Long slotId, Long storeId) {
        BookingSlot slot = requireSlot(slotId);
        if (!slot.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_NOT_FOUND);
        }
        if (!SLOT_OPEN.equals(slot.getStatus())) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_UNAVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (slot.getServiceDate().isBefore(now.toLocalDate())) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_UNAVAILABLE.getCode(), "不能预约过去的日期");
        }
        if (slot.getServiceDate().equals(now.toLocalDate())
                && !slot.getEndTime().isAfter(now.toLocalTime())) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_UNAVAILABLE.getCode(), "该时段已过期");
        }
        if (availableCapacity(slot) <= 0) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_FULL);
        }
        return slot;
    }

    private CommerceStore requireOpenStore(Long storeId) {
        CommerceStore store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "门店不存在");
        }
        if (!"OPEN".equals(store.getBusinessStatus())) {
            throw new BusinessException(ErrorCode.COMMERCE_STORE_NOT_AVAILABLE);
        }
        return store;
    }

    private void validateStaff(Long storeId, Long staffId) {
        if (staffId == null || staffId == 0L) {
            return;
        }
        CommerceStaff staff = staffMapper.selectById(staffId);
        if (staff == null || !staff.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "员工不属于该门店");
        }
    }

    private void validateSlotTimes(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "结束时间必须晚于开始时间");
        }
    }

    private void validateSlotDate(LocalDate serviceDate) {
        if (serviceDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "不能创建或修改过去的时段");
        }
    }

    private String normalizeSlotStatus(String status) {
        if (status == null || status.isBlank()) {
            return SLOT_OPEN;
        }
        String value = status.trim().toUpperCase();
        if (!SLOT_OPEN.equals(value) && !SLOT_CLOSED.equals(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "时段状态仅支持 OPEN/CLOSED");
        }
        return value;
    }

    private String normalizePayType(String payType) {
        String value = payType != null ? payType.trim().toUpperCase() : "";
        if ("CASH".equals(value)) {
            return PAY_TYPE_ONLINE;
        }
        if (!PAY_TYPE_ONLINE.equals(value) && !PAY_TYPE_PACKAGE.equals(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(),
                    "支付方式仅支持 ONLINE/PACKAGE");
        }
        return value;
    }

    private int availableCapacity(BookingSlot slot) {
        return safeInt(slot.getTotalCapacity()) - safeInt(slot.getReservedCapacity())
                - safeInt(slot.getLockedCapacity());
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private Long resolveStaffId(BookingSlot slot) {
        return slot.getStaffId() != null && slot.getStaffId() > 0 ? slot.getStaffId() : null;
    }

    private BusinessException statusError(GroomingBooking booking) {
        return new BusinessException(ErrorCode.BOOKING_STATUS_ERROR.getCode(),
                "预约当前状态为 " + booking.getStatus() + "，不允许此操作");
    }

    private void writeBookingLog(Long bookingId, String from, String to, String action,
                                 String operatorType, Long operatorId, String remark) {
        bookingLogMapper.insert(GroomingBookingLog.builder()
                .bookingId(bookingId)
                .fromStatus(from)
                .toStatus(to)
                .action(action)
                .operatorType(operatorType)
                .operatorId(operatorId)
                .remark(remark)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Map<String, Object> buildBookingSpecSnapshot(String bookingNo, GroomingService service,
                                                         CommerceStore store, BookingSlot slot,
                                                         Long petId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("bookingNo", bookingNo);
        snapshot.put("serviceId", service.getId());
        snapshot.put("serviceName", service.getName());
        snapshot.put("durationMin", service.getDurationMin());
        snapshot.put("storeId", store.getId());
        snapshot.put("storeName", store.getName());
        snapshot.put("serviceDate", slot.getServiceDate().toString());
        snapshot.put("startTime", slot.getStartTime().toString());
        snapshot.put("endTime", slot.getEndTime().toString());
        snapshot.put("petId", petId);
        return snapshot;
    }

    private String toStoreSnapshot(CommerceStore store) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", store.getId());
        snapshot.put("storeCode", store.getStoreCode());
        snapshot.put("name", store.getName());
        snapshot.put("address", store.getAddress());
        return toJson(snapshot);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "预约快照序列化失败");
        }
    }

    private String generateBookingNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "BK" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

}
