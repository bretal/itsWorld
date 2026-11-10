package com.itsworld.commerce.packagebiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.commerce.booking.dto.PackageBookingOptionView;
import com.itsworld.commerce.booking.entity.GroomingBooking;
import com.itsworld.commerce.booking.mapper.GroomingBookingMapper;
import com.itsworld.commerce.common.CommercePriceUtils;
import com.itsworld.commerce.grooming.entity.GroomingService;
import com.itsworld.commerce.grooming.mapper.GroomingServiceMapper;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.entity.CommerceOrderDiscount;
import com.itsworld.commerce.order.entity.CommerceOrderItem;
import com.itsworld.commerce.order.entity.CommerceOrderStatusLog;
import com.itsworld.commerce.order.event.CommerceOrderStatusChangedEvent;
import com.itsworld.commerce.order.mapper.CommerceOrderDiscountMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderItemMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderStatusLogMapper;
import com.itsworld.commerce.packagebiz.dto.PackageItemRequest;
import com.itsworld.commerce.packagebiz.dto.PackageItemView;
import com.itsworld.commerce.packagebiz.dto.PackageOptionItemRequest;
import com.itsworld.commerce.packagebiz.dto.PackageOptionItemView;
import com.itsworld.commerce.packagebiz.dto.PackageOptionRequest;
import com.itsworld.commerce.packagebiz.dto.PackageOptionView;
import com.itsworld.commerce.packagebiz.dto.PackagePurchasePreview;
import com.itsworld.commerce.packagebiz.dto.PackagePurchasePreviewRequest;
import com.itsworld.commerce.packagebiz.dto.PackagePurchaseRequest;
import com.itsworld.commerce.packagebiz.dto.PackagePurchaseResult;
import com.itsworld.commerce.packagebiz.dto.PackageSelectionItemView;
import com.itsworld.commerce.packagebiz.dto.PackageTemplateSaveRequest;
import com.itsworld.commerce.packagebiz.dto.PackageTemplateView;
import com.itsworld.commerce.packagebiz.dto.PackageUsageView;
import com.itsworld.commerce.packagebiz.dto.UserPackageItemView;
import com.itsworld.commerce.packagebiz.dto.UserPackageView;
import com.itsworld.commerce.packagebiz.entity.ServicePackageOption;
import com.itsworld.commerce.packagebiz.entity.ServicePackageOptionItem;
import com.itsworld.commerce.packagebiz.entity.ServicePackageTemplate;
import com.itsworld.commerce.packagebiz.entity.ServicePackageTemplateItem;
import com.itsworld.commerce.packagebiz.entity.ServicePackageUsage;
import com.itsworld.commerce.packagebiz.entity.UserServicePackage;
import com.itsworld.commerce.packagebiz.entity.UserServicePackageItem;
import com.itsworld.commerce.packagebiz.mapper.ServicePackageOptionItemMapper;
import com.itsworld.commerce.packagebiz.mapper.ServicePackageOptionMapper;
import com.itsworld.commerce.packagebiz.mapper.ServicePackageTemplateItemMapper;
import com.itsworld.commerce.packagebiz.mapper.ServicePackageTemplateMapper;
import com.itsworld.commerce.packagebiz.mapper.ServicePackageUsageMapper;
import com.itsworld.commerce.packagebiz.mapper.UserServicePackageItemMapper;
import com.itsworld.commerce.packagebiz.mapper.UserServicePackageMapper;
import com.itsworld.commerce.store.entity.CommerceStore;
import com.itsworld.commerce.store.mapper.CommerceStoreMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.member.dto.CouponOptionView;
import com.itsworld.member.dto.CouponQuote;
import com.itsworld.member.service.CouponService;
import com.itsworld.member.service.MemberService;
import com.itsworld.pet.dto.PetResponse;
import com.itsworld.pet.entity.Pet;
import com.itsworld.pet.service.PetService;
import com.itsworld.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 服务套餐业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final String ACTIVE = "ACTIVE";
    private static final String USED_UP = "USED_UP";
    private static final String EXPIRED = "EXPIRED";
    private static final String PACKAGE_ITEM_TYPE = "PACKAGE";
    private static final String ORDER_TYPE_PACKAGE = "PACKAGE";
    private static final int PAY_EXPIRE_MINUTES = 15;

    private final ServicePackageTemplateMapper templateMapper;
    private final ServicePackageTemplateItemMapper templateItemMapper;
    private final ServicePackageOptionMapper optionMapper;
    private final ServicePackageOptionItemMapper optionItemMapper;
    private final UserServicePackageMapper userPackageMapper;
    private final UserServicePackageItemMapper userPackageItemMapper;
    private final ServicePackageUsageMapper usageMapper;
    private final GroomingServiceMapper groomingServiceMapper;
    private final GroomingBookingMapper bookingMapper;
    private final CommerceStoreMapper storeMapper;
    private final CommerceOrderMapper orderMapper;
    private final CommerceOrderItemMapper orderItemMapper;
    private final CommerceOrderDiscountMapper orderDiscountMapper;
    private final CommerceOrderStatusLogMapper orderStatusLogMapper;
    private final MemberService memberService;
    private final CouponService couponService;
    private final WalletService walletService;
    private final PetService petService;
    private final ObjectMapper objectMapper;

    public PageResult<PackageTemplateView> pageTemplates(String keyword, long pageNum, long pageSize) {
        Page<ServicePackageTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServicePackageTemplate> wrapper =
                new LambdaQueryWrapper<ServicePackageTemplate>()
                        .eq(ServicePackageTemplate::getStatus, ENABLED)
                        .orderByDesc(ServicePackageTemplate::getId);
        applyKeyword(wrapper, keyword);
        Page<ServicePackageTemplate> result = templateMapper.selectPage(page, wrapper);
        Map<Long, String> serviceNames = serviceNameMap();
        List<PackageTemplateView> records = new ArrayList<>();
        for (ServicePackageTemplate template : result.getRecords()) {
            records.add(buildTemplateView(template, serviceNames));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PackageTemplateView getTemplateDetail(Long templateId) {
        ServicePackageTemplate template = requireEnabledTemplate(templateId);
        return buildTemplateView(template, serviceNameMap());
    }

    public List<PackageTemplateView> listEnabledTemplates() {
        Map<Long, String> serviceNames = serviceNameMap();
        List<ServicePackageTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<ServicePackageTemplate>()
                        .eq(ServicePackageTemplate::getStatus, ENABLED)
                        .orderByDesc(ServicePackageTemplate::getId));
        List<PackageTemplateView> views = new ArrayList<>();
        for (ServicePackageTemplate template : templates) {
            views.add(buildTemplateView(template, serviceNames));
        }
        return views;
    }

    public PackagePurchasePreview previewPurchase(Long userId,
                                                  PackagePurchasePreviewRequest request) {
        ServicePackageTemplate template = requireEnabledTemplate(request.getTemplateId());
        SelectionResult selection = resolveSelection(template, request.getOptionItemIds(), false);
        long originalAmount = template.getSalePrice() != null ? template.getSalePrice() : 0L;
        for (PackageSelectionItemView item : selection.items()) {
            if (Boolean.TRUE.equals(item.getFromOption()) && item.getAddPrice() != null) {
                originalAmount += item.getAddPrice();
            }
        }
        BigDecimal discountRate = memberService.getCurrentDiscountRate(userId);
        long memberTotal = CommercePriceUtils.applyDiscount(originalAmount, discountRate);
        long memberDiscount = originalAmount - memberTotal;

        CouponQuote couponQuote = request.getCouponId() != null
                ? couponService.quoteCoupon(userId, request.getCouponId(), memberTotal) : null;
        long couponDiscount = couponQuote != null ? couponQuote.getDiscountAmount() : 0L;
        long payableAmount = Math.max(0L, memberTotal - couponDiscount);
        List<CouponOptionView> couponOptions = couponService.listCouponOptions(userId, memberTotal);

        return PackagePurchasePreview.builder()
                .templateId(template.getId())
                .packageCode(template.getPackageCode())
                .packageName(template.getName())
                .originalAmount(originalAmount)
                .memberDiscount(memberDiscount)
                .couponDiscount(couponDiscount)
                .payableAmount(payableAmount)
                .walletAvailableBalance(walletService.getSummary(userId).getAvailableBalance())
                .items(selection.items())
                .couponOptions(couponOptions)
                .build();
    }

    @Transactional
    public PackagePurchaseResult purchasePackage(Long userId, PackagePurchaseRequest request) {
        CommerceOrder existing = orderMapper.selectByUserAndIdempotency(
                userId, request.getIdempotencyKey());
        if (existing != null) {
            return PackagePurchaseResult.builder()
                    .orderId(existing.getId())
                    .orderNo(existing.getOrderNo())
                    .payableAmount(existing.getPayableAmount())
                    .expireTime(existing.getExpireTime())
                    .build();
        }
        ServicePackageTemplate template = requireEnabledTemplate(request.getTemplateId());
        PetResponse pet = petService.getPetInfo(userId, request.getPetId());
        CommerceStore store = requireOpenStore(request.getStoreId());
        SelectionResult selection = resolveSelection(template, request.getOptionItemIds(), true);
        PackagePurchasePreview preview = previewPurchase(userId,
                PackagePurchasePreviewRequest.builder()
                        .templateId(request.getTemplateId())
                        .optionItemIds(request.getOptionItemIds())
                        .couponId(request.getCouponId())
                        .build());
        if (preview.getPayableAmount() == null || preview.getPayableAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "套餐应付金额必须大于0");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(PAY_EXPIRE_MINUTES);
        String orderNo = generateOrderNo();
        long couponDiscount = preview.getCouponDiscount() != null ? preview.getCouponDiscount() : 0L;
        String specSnapshot = buildSelectionSnapshot(template, selection);

        CommerceOrder order = CommerceOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .orderType(ORDER_TYPE_PACKAGE)
                .orderStatus("WAIT_PAY")
                .paymentStatus("UNPAID")
                .fulfillmentStatus("PENDING")
                .originalAmount(preview.getOriginalAmount())
                .discountAmount(preview.getMemberDiscount() + couponDiscount)
                .payableAmount(preview.getPayableAmount())
                .paidAmount(0L)
                .refundedAmount(0L)
                .couponId(request.getCouponId())
                .storeSnapshot(toStoreSnapshot(store))
                .expireTime(expireTime)
                .remark("套餐购买：" + pet.getPetName())
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(now)
                .updatedAt(now)
                .build();
        orderMapper.insert(order);

        CommerceOrderItem orderItem = CommerceOrderItem.builder()
                .orderId(order.getId())
                .itemType(PACKAGE_ITEM_TYPE)
                .targetId(template.getId())
                .petId(request.getPetId())
                .quantity(1)
                .titleSnapshot(template.getName())
                .specSnapshot(specSnapshot)
                .unitPrice(preview.getOriginalAmount())
                .originalTotal(preview.getOriginalAmount())
                .discountAllocated(preview.getMemberDiscount() + couponDiscount)
                .payableTotal(preview.getPayableAmount())
                .fulfilledQty(0)
                .refundedAmount(0L)
                .createdAt(now)
                .build();
        orderItemMapper.insert(orderItem);

        if (preview.getMemberDiscount() > 0) {
            orderDiscountMapper.insert(CommerceOrderDiscount.builder()
                    .orderId(order.getId())
                    .orderItemId(orderItem.getId())
                    .discountType("MEMBER")
                    .discountAmount(preview.getMemberDiscount())
                    .ruleSnapshot(toJson(Map.of("discountRate",
                            memberService.getCurrentDiscountRate(userId))))
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

        orderStatusLogMapper.insert(CommerceOrderStatusLog.builder()
                .orderId(order.getId())
                .fromStatus(null)
                .toStatus("WAIT_PAY")
                .action("CREATE")
                .operatorType("USER")
                .operatorId(userId)
                .remark("提交套餐订单")
                .createdAt(now)
                .build());

        log.info("套餐订单创建: orderNo={}, userId={}, templateId={}, payable={}",
                orderNo, userId, template.getId(), order.getPayableAmount());
        return PackagePurchaseResult.builder()
                .orderId(order.getId())
                .orderNo(orderNo)
                .payableAmount(order.getPayableAmount())
                .expireTime(expireTime)
                .build();
    }

    public PageResult<UserPackageView> pageMyPackages(Long userId, String status,
                                                      long pageNum, long pageSize) {
        Page<UserServicePackage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserServicePackage> wrapper =
                new LambdaQueryWrapper<UserServicePackage>()
                        .eq(UserServicePackage::getUserId, userId)
                        .orderByDesc(UserServicePackage::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(UserServicePackage::getStatus, status.trim().toUpperCase());
        }
        Page<UserServicePackage> result = userPackageMapper.selectPage(page, wrapper);
        List<UserPackageView> records = new ArrayList<>();
        for (UserServicePackage userPackage : result.getRecords()) {
            records.add(buildUserPackageView(userPackage));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public UserPackageView getMyPackage(Long userId, Long userPackageId) {
        UserServicePackage userPackage = requireOwnedPackage(userId, userPackageId);
        return buildUserPackageView(userPackage);
    }

    public List<PackageUsageView> listUsages(Long userId, Long userPackageId) {
        requireOwnedPackage(userId, userPackageId);
        List<ServicePackageUsage> usages = usageMapper.selectByUserPackageId(userPackageId);
        Map<Long, String> serviceNames = serviceNameMap();
        List<PackageUsageView> views = new ArrayList<>();
        for (ServicePackageUsage usage : usages) {
            GroomingBooking booking = usage.getBookingId() != null
                    ? bookingMapper.selectById(usage.getBookingId()) : null;
            views.add(PackageUsageView.builder()
                    .id(usage.getId())
                    .usageNo(usage.getUsageNo())
                    .userPackageId(usage.getUserPackageId())
                    .packageItemId(usage.getPackageItemId())
                    .bookingId(usage.getBookingId())
                    .bookingNo(booking != null ? booking.getBookingNo() : null)
                    .serviceId(usage.getServiceId())
                    .serviceName(serviceNames.get(usage.getServiceId()))
                    .qty(usage.getQty())
                    .status(usage.getStatus())
                    .usedTime(usage.getUsedTime())
                    .createdAt(usage.getCreatedAt())
                    .build());
        }
        return views;
    }

    public PageResult<PackageTemplateView> pageAdminTemplates(String status, String keyword,
                                                              long pageNum, long pageSize) {
        Page<ServicePackageTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServicePackageTemplate> wrapper =
                new LambdaQueryWrapper<ServicePackageTemplate>()
                        .orderByDesc(ServicePackageTemplate::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(ServicePackageTemplate::getStatus, status.trim().toUpperCase());
        }
        applyKeyword(wrapper, keyword);
        Page<ServicePackageTemplate> result = templateMapper.selectPage(page, wrapper);
        Map<Long, String> serviceNames = serviceNameMap();
        List<PackageTemplateView> records = new ArrayList<>();
        for (ServicePackageTemplate template : result.getRecords()) {
            records.add(buildTemplateView(template, serviceNames));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PackageTemplateView getAdminTemplate(Long templateId) {
        return buildTemplateView(requireTemplate(templateId), serviceNameMap());
    }

    @Transactional
    public PackageTemplateView createTemplate(PackageTemplateSaveRequest request) {
        validateTemplateRequest(request, null);
        ServicePackageTemplate template = ServicePackageTemplate.builder()
                .packageCode(request.getPackageCode().trim())
                .name(request.getName().trim())
                .coverUrl(request.getCoverUrl())
                .description(request.getDescription())
                .salePrice(request.getSalePrice())
                .validDays(request.getValidDays())
                .status(normalizeStatus(request.getStatus()))
                .allowCrossStore(request.getAllowCrossStore() != null
                        ? request.getAllowCrossStore() : 0)
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        templateMapper.insert(template);
        saveTemplateChildren(template.getId(), request);
        return getAdminTemplate(template.getId());
    }

    @Transactional
    public PackageTemplateView updateTemplate(Long templateId, PackageTemplateSaveRequest request) {
        ServicePackageTemplate template = requireTemplate(templateId);
        validateTemplateRequest(request, templateId);
        template.setPackageCode(request.getPackageCode().trim());
        template.setName(request.getName().trim());
        template.setCoverUrl(request.getCoverUrl());
        template.setDescription(request.getDescription());
        template.setSalePrice(request.getSalePrice());
        template.setValidDays(request.getValidDays());
        template.setStatus(normalizeStatus(request.getStatus()));
        template.setAllowCrossStore(request.getAllowCrossStore() != null
                ? request.getAllowCrossStore() : template.getAllowCrossStore());
        template.setVersion(template.getVersion() != null ? template.getVersion() + 1 : 1);
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);

        templateItemMapper.delete(new LambdaQueryWrapper<ServicePackageTemplateItem>()
                .eq(ServicePackageTemplateItem::getPackageId, templateId));
        optionMapper.delete(new LambdaQueryWrapper<ServicePackageOption>()
                .eq(ServicePackageOption::getPackageId, templateId));
        saveTemplateChildren(templateId, request);
        return getAdminTemplate(templateId);
    }

    @Transactional
    public void changeTemplateStatus(Long templateId, String status) {
        ServicePackageTemplate template = requireTemplate(templateId);
        template.setStatus(normalizeStatus(status));
        template.setVersion(template.getVersion() != null ? template.getVersion() + 1 : 1);
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    public List<PackageBookingOptionView> listBookingOptions(Long userId, Long serviceId,
                                                             Long storeId) {
        List<UserServicePackage> packages = userPackageMapper.selectList(
                new LambdaQueryWrapper<UserServicePackage>()
                        .eq(UserServicePackage::getUserId, userId)
                        .eq(UserServicePackage::getStatus, ACTIVE)
                        .orderByAsc(UserServicePackage::getExpireTime));
        List<PackageBookingOptionView> options = new ArrayList<>();
        for (UserServicePackage userPackage : packages) {
            if (!isPackageActive(userPackage)) {
                continue;
            }
            ServicePackageTemplate template = requireTemplate(userPackage.getPackageId());
            boolean crossStore = template.getAllowCrossStore() != null
                    && template.getAllowCrossStore() == 1;
            if (storeId != null && !crossStore && userPackage.getStoreId() != null
                    && !userPackage.getStoreId().equals(storeId)) {
                continue;
            }
            List<UserServicePackageItem> items = userPackageItemMapper.selectList(
                    new LambdaQueryWrapper<UserServicePackageItem>()
                            .eq(UserServicePackageItem::getUserPackageId, userPackage.getId())
                            .eq(UserServicePackageItem::getServiceId, serviceId));
            for (UserServicePackageItem item : items) {
                int remaining = remainingTimes(item);
                if (remaining <= 0) {
                    continue;
                }
                options.add(PackageBookingOptionView.builder()
                        .userPackageItemId(item.getId())
                        .userPackageId(userPackage.getId())
                        .packageInstanceNo(userPackage.getPackageInstanceNo())
                        .packageName(userPackage.getNameSnapshot())
                        .petId(userPackage.getPetId())
                        .petName(petName(userPackage.getPetId()))
                        .storeId(userPackage.getStoreId())
                        .storeName(storeName(userPackage.getStoreId()))
                        .serviceId(item.getServiceId())
                        .serviceName(serviceNames().get(item.getServiceId()))
                        .totalTimes(item.getTotalTimes())
                        .usedTimes(item.getUsedTimes())
                        .frozenTimes(item.getFrozenTimes())
                        .remainingTimes(remaining)
                        .expireTime(userPackage.getExpireTime())
                        .build());
            }
        }
        return options;
    }

    @Transactional
    public void freezeForBooking(Long userId, Long userPackageItemId, Long bookingId,
                                 Long serviceId, Long storeId, Long petId) {
        UserServicePackageItem item = userPackageItemMapper.selectById(userPackageItemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.USER_PACKAGE_NOT_FOUND);
        }
        UserServicePackage userPackage = requireOwnedPackage(userId, item.getUserPackageId());
        if (!isPackageActive(userPackage)) {
            throw new BusinessException(ErrorCode.USER_PACKAGE_NOT_AVAILABLE);
        }
        if (!item.getServiceId().equals(serviceId)) {
            throw new BusinessException(ErrorCode.USER_PACKAGE_NOT_AVAILABLE.getCode(),
                    "套餐服务项与预约服务不一致");
        }
        if (userPackage.getPetId() != null && !userPackage.getPetId().equals(petId)) {
            throw new BusinessException(ErrorCode.USER_PACKAGE_NOT_AVAILABLE.getCode(),
                    "该套餐仅限绑定宠物使用");
        }
        ServicePackageTemplate template = requireTemplate(userPackage.getPackageId());
        boolean crossStore = template.getAllowCrossStore() != null
                && template.getAllowCrossStore() == 1;
        if (!crossStore && userPackage.getStoreId() != null
                && !userPackage.getStoreId().equals(storeId)) {
            throw new BusinessException(ErrorCode.USER_PACKAGE_NOT_AVAILABLE.getCode(),
                    "该套餐不支持跨店使用");
        }
        String idempotencyKey = "BOOKING_PACKAGE:" + bookingId;
        ServicePackageUsage existing = usageMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return;
        }
        int frozen = userPackageItemMapper.freezeTimes(item.getId(), 1);
        if (frozen != 1) {
            throw new BusinessException(ErrorCode.PACKAGE_TIMES_INSUFFICIENT);
        }
        try {
            usageMapper.insert(ServicePackageUsage.builder()
                    .usageNo(generateUsageNo())
                    .userPackageId(userPackage.getId())
                    .packageItemId(item.getId())
                    .bookingId(bookingId)
                    .serviceId(serviceId)
                    .qty(1)
                    .status("FROZEN")
                    .idempotencyKey(idempotencyKey)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        } catch (DuplicateKeyException e) {
            log.warn("套餐核销流水重复: bookingId={}", bookingId);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "预约提交重复，请刷新后重试");
        }
    }

    @Transactional
    public void consumeForBooking(Long bookingId, Long operatorId) {
        ServicePackageUsage usage = usageMapper.selectFrozenByBookingId(bookingId);
        if (usage == null) {
            return;
        }
        int consumed = userPackageItemMapper.consumeFrozenTimes(usage.getPackageItemId(), usage.getQty());
        if (consumed != 1) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "套餐次数确认失败");
        }
        if (usageMapper.markUsed(usage.getId()) != 1) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "套餐核销状态更新失败");
        }
        if (operatorId != null) {
            usageMapper.update(null, new LambdaUpdateWrapper<ServicePackageUsage>()
                    .eq(ServicePackageUsage::getId, usage.getId())
                    .set(ServicePackageUsage::getOperatorId, operatorId));
        }
        markUsedUpIfNecessary(usage.getUserPackageId());
    }

    @Transactional
    public void releaseForBooking(Long bookingId) {
        ServicePackageUsage usage = usageMapper.selectFrozenByBookingId(bookingId);
        if (usage == null) {
            return;
        }
        int released = userPackageItemMapper.releaseFrozenTimes(usage.getPackageItemId(), usage.getQty());
        if (released != 1) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "套餐次数释放失败");
        }
        if (usageMapper.markReleased(usage.getId()) != 1) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "套餐核销状态更新失败");
        }
    }

    @Transactional
    public void issueForPaidOrder(Long orderId) {
        CommerceOrder order = orderMapper.selectById(orderId);
        if (order == null || !ORDER_TYPE_PACKAGE.equals(order.getOrderType())) {
            return;
        }
        List<CommerceOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<CommerceOrderItem>()
                        .eq(CommerceOrderItem::getOrderId, orderId)
                        .eq(CommerceOrderItem::getItemType, PACKAGE_ITEM_TYPE));
        for (CommerceOrderItem item : items) {
            if (userPackageMapper.selectByOrderItemId(item.getId()) != null) {
                continue;
            }
            issuePackage(order, item);
        }
        orderMapper.completeIfPaid(orderId);
    }

    private void issuePackage(CommerceOrder order, CommerceOrderItem orderItem) {
        JsonNode snapshot = readSnapshot(orderItem.getSpecSnapshot());
        ServicePackageTemplate template = requireTemplate(orderItem.getTargetId());
        LocalDateTime now = LocalDateTime.now();
        int validDays = snapshot.hasNonNull("validDays") ? snapshot.path("validDays").asInt(0) : 0;
        LocalDateTime expireTime = validDays > 0 ? now.plusDays(validDays) : null;

        UserServicePackage userPackage = UserServicePackage.builder()
                .packageInstanceNo(generateInstanceNo())
                .userId(order.getUserId())
                .petId(orderItem.getPetId())
                .packageId(template.getId())
                .orderItemId(orderItem.getId())
                .storeId(resolveOrderStoreId(order))
                .nameSnapshot(orderItem.getTitleSnapshot())
                .selectionSnapshot(orderItem.getSpecSnapshot())
                .startTime(now)
                .expireTime(expireTime)
                .status(ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userPackageMapper.insert(userPackage);

        JsonNode itemsNode = snapshot.path("items");
        for (JsonNode node : itemsNode) {
            long serviceId = node.path("serviceId").asLong();
            int times = node.path("times").asInt(0);
            if (serviceId <= 0 || times <= 0) {
                continue;
            }
            userPackageItemMapper.insert(UserServicePackageItem.builder()
                    .userPackageId(userPackage.getId())
                    .serviceId(serviceId)
                    .totalTimes(times)
                    .usedTimes(0)
                    .frozenTimes(0)
                    .serviceSnapshot(node.hasNonNull("serviceSnapshot")
                            ? node.path("serviceSnapshot").toString() : null)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        log.info("套餐发放: orderNo={}, instanceNo={}, userId={}",
                order.getOrderNo(), userPackage.getPackageInstanceNo(), order.getUserId());
    }

    @EventListener
    public void onOrderStatusChanged(CommerceOrderStatusChangedEvent event) {
        if (CommerceOrderStatusChangedEvent.ACTION_PAID.equals(event.getAction())
                && ORDER_TYPE_PACKAGE.equals(event.getOrder().getOrderType())) {
            issueForPaidOrder(event.getOrder().getId());
        }
    }

    private Long resolveOrderStoreId(CommerceOrder order) {
        if (!StringUtils.hasText(order.getStoreSnapshot())) {
            return null;
        }
        try {
            JsonNode store = objectMapper.readTree(order.getStoreSnapshot());
            long storeId = store.path("id").asLong(0);
            return storeId > 0 ? storeId : null;
        } catch (Exception e) {
            log.warn("解析套餐订单门店快照失败: orderNo={}", order.getOrderNo());
            return null;
        }
    }

    private SelectionResult resolveSelection(ServicePackageTemplate template, List<Long> optionItemIds,
                                             boolean strict) {
        List<ServicePackageTemplateItem> fixedItems = templateItemMapper.selectList(
                new LambdaQueryWrapper<ServicePackageTemplateItem>()
                        .eq(ServicePackageTemplateItem::getPackageId, template.getId())
                        .orderByAsc(ServicePackageTemplateItem::getSort)
                        .orderByAsc(ServicePackageTemplateItem::getId));
        List<ServicePackageOption> options = optionMapper.selectList(
                new LambdaQueryWrapper<ServicePackageOption>()
                        .eq(ServicePackageOption::getPackageId, template.getId())
                        .orderByAsc(ServicePackageOption::getSort)
                        .orderByAsc(ServicePackageOption::getId));
        List<Long> requestedIds = optionItemIds != null ? optionItemIds : List.of();
        Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new BusinessException(ErrorCode.PACKAGE_SELECTION_INVALID.getCode(), "选项不能重复选择");
        }
        List<ServicePackageOptionItem> selectedItems = uniqueIds.isEmpty()
                ? List.of()
                : optionItemMapper.selectBatchIds(uniqueIds);
        if (selectedItems.size() != uniqueIds.size()) {
            throw new BusinessException(ErrorCode.PACKAGE_SELECTION_INVALID);
        }
        Map<Long, String> serviceNames = serviceNameMap();
        Map<Long, List<ServicePackageOptionItem>> selectedByOption = new HashMap<>();
        for (ServicePackageOptionItem item : selectedItems) {
            if (!ENABLED.equals(item.getStatus())) {
                throw new BusinessException(ErrorCode.PACKAGE_SELECTION_INVALID.getCode(), "选项服务已停用");
            }
            selectedByOption.computeIfAbsent(item.getOptionId(), key -> new ArrayList<>()).add(item);
        }

        List<PackageSelectionItemView> views = new ArrayList<>();
        for (ServicePackageTemplateItem fixed : fixedItems) {
            views.add(PackageSelectionItemView.builder()
                    .serviceId(fixed.getServiceId())
                    .serviceName(serviceNames.get(fixed.getServiceId()))
                    .times(fixed.getIncludedTimes())
                    .addPrice(0L)
                    .fromOption(false)
                    .build());
        }

        Set<Long> knownOptionIds = new LinkedHashSet<>();
        for (ServicePackageOption option : options) {
            knownOptionIds.add(option.getId());
            List<ServicePackageOptionItem> chosen =
                    selectedByOption.getOrDefault(option.getId(), List.of());
            int minSelect = option.getMinSelect() != null ? option.getMinSelect() : 0;
            int maxSelect = option.getMaxSelect() != null ? option.getMaxSelect() : 0;
            boolean required = option.getRequired() != null && option.getRequired() == 1;
            if (strict && (required ? chosen.size() < Math.max(1, minSelect)
                    : chosen.size() < minSelect)) {
                throw new BusinessException(ErrorCode.PACKAGE_SELECTION_INVALID.getCode(),
                        "选项「" + option.getOptionName() + "」选择数量不足");
            }
            if (maxSelect > 0 && chosen.size() > maxSelect) {
                throw new BusinessException(ErrorCode.PACKAGE_SELECTION_INVALID.getCode(),
                        "选项「" + option.getOptionName() + "」最多选择 " + maxSelect + " 项");
            }
            for (ServicePackageOptionItem item : chosen) {
                views.add(PackageSelectionItemView.builder()
                        .serviceId(item.getServiceId())
                        .serviceName(serviceNames.get(item.getServiceId()))
                        .times(item.getIncludedTimes())
                        .addPrice(item.getAddPrice() != null ? item.getAddPrice() : 0L)
                        .fromOption(true)
                        .build());
            }
        }
        for (Long optionId : selectedByOption.keySet()) {
            if (!knownOptionIds.contains(optionId)) {
                throw new BusinessException(ErrorCode.PACKAGE_SELECTION_INVALID.getCode(),
                        "选项不属于该套餐");
            }
        }
        return new SelectionResult(views, selectedItems, options);
    }

    private String buildSelectionSnapshot(ServicePackageTemplate template, SelectionResult selection) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("templateId", template.getId());
        snapshot.put("templateName", template.getName());
        snapshot.put("validDays", template.getValidDays());
        Map<Long, Integer> timesMap = new LinkedHashMap<>();
        Map<Long, Map<String, Object>> serviceMap = new LinkedHashMap<>();
        for (PackageSelectionItemView item : selection.items()) {
            timesMap.merge(item.getServiceId(), item.getTimes() != null ? item.getTimes() : 1,
                    Integer::sum);
            GroomingService service = groomingServiceMapper.selectById(item.getServiceId());
            if (service != null) {
                Map<String, Object> serviceSnapshot = new LinkedHashMap<>();
                serviceSnapshot.put("serviceCode", service.getServiceCode());
                serviceSnapshot.put("name", service.getName());
                serviceSnapshot.put("coverUrl", service.getCoverUrl());
                serviceSnapshot.put("durationMin", service.getDurationMin());
                serviceMap.put(item.getServiceId(), serviceSnapshot);
            }
        }
        List<Map<String, Object>> itemSnapshots = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : timesMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("serviceId", entry.getKey());
            item.put("times", entry.getValue());
            item.put("serviceSnapshot", serviceMap.get(entry.getKey()));
            itemSnapshots.add(item);
        }
        snapshot.put("items", itemSnapshots);

        List<Map<String, Object>> selectionSnapshots = new ArrayList<>();
        Map<Long, ServicePackageOption> optionMap = new HashMap<>();
        for (ServicePackageOption option : selection.options()) {
            optionMap.put(option.getId(), option);
        }
        for (ServicePackageOptionItem item : selection.selectedItems()) {
            ServicePackageOption option = optionMap.get(item.getOptionId());
            Map<String, Object> selected = new LinkedHashMap<>();
            selected.put("optionId", item.getOptionId());
            selected.put("optionName", option != null ? option.getOptionName() : null);
            selected.put("optionItemId", item.getId());
            selected.put("serviceId", item.getServiceId());
            selected.put("addPrice", item.getAddPrice());
            selected.put("includedTimes", item.getIncludedTimes());
            selectionSnapshots.add(selected);
        }
        snapshot.put("selection", selectionSnapshots);
        return toJson(snapshot);
    }

    private void saveTemplateChildren(Long templateId, PackageTemplateSaveRequest request) {
        int sort = 0;
        if (request.getItems() != null) {
            for (PackageItemRequest item : request.getItems()) {
                templateItemMapper.insert(ServicePackageTemplateItem.builder()
                        .packageId(templateId)
                        .serviceId(item.getServiceId())
                        .includedTimes(item.getIncludedTimes())
                        .selectableGroup(item.getSelectableGroup())
                        .extraPrice(item.getExtraPrice() != null ? item.getExtraPrice() : 0L)
                        .sort(item.getSort() != null ? item.getSort() : sort)
                        .createdAt(LocalDateTime.now())
                        .build());
                sort++;
            }
        }
        if (request.getOptions() == null) {
            return;
        }
        int optionSort = 0;
        for (PackageOptionRequest optionRequest : request.getOptions()) {
            ServicePackageOption option = ServicePackageOption.builder()
                    .packageId(templateId)
                    .optionCode(optionRequest.getOptionCode().trim())
                    .optionName(optionRequest.getOptionName().trim())
                    .minSelect(optionRequest.getMinSelect())
                    .maxSelect(optionRequest.getMaxSelect())
                    .required(optionRequest.getRequired() != null ? optionRequest.getRequired() : 0)
                    .sort(optionRequest.getSort() != null ? optionRequest.getSort() : optionSort)
                    .createdAt(LocalDateTime.now())
                    .build();
            optionMapper.insert(option);
            if (optionRequest.getItems() != null) {
                for (PackageOptionItemRequest itemRequest : optionRequest.getItems()) {
                    optionItemMapper.insert(ServicePackageOptionItem.builder()
                            .optionId(option.getId())
                            .serviceId(itemRequest.getServiceId())
                            .addPrice(itemRequest.getAddPrice() != null
                                    ? itemRequest.getAddPrice() : 0L)
                            .includedTimes(itemRequest.getIncludedTimes())
                            .status(normalizeStatus(itemRequest.getStatus()))
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            }
            optionSort++;
        }
    }

    private void validateTemplateRequest(PackageTemplateSaveRequest request, Long excludeId) {
        LambdaQueryWrapper<ServicePackageTemplate> wrapper =
                new LambdaQueryWrapper<ServicePackageTemplate>()
                        .eq(ServicePackageTemplate::getPackageCode, request.getPackageCode().trim());
        if (excludeId != null) {
            wrapper.ne(ServicePackageTemplate::getId, excludeId);
        }
        Long count = templateMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "套餐编码已存在");
        }
        if (request.getValidDays() != null && request.getValidDays() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "有效天数必须大于0");
        }
        boolean hasItems = request.getItems() != null && !request.getItems().isEmpty();
        boolean hasOptions = request.getOptions() != null && !request.getOptions().isEmpty();
        if (!hasItems && !hasOptions) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "套餐至少包含一个服务项或选项组");
        }
        if (request.getItems() != null) {
            for (PackageItemRequest item : request.getItems()) {
                requireGroomingService(item.getServiceId());
            }
        }
        if (request.getOptions() == null) {
            return;
        }
        Set<String> optionCodes = new LinkedHashSet<>();
        for (PackageOptionRequest option : request.getOptions()) {
            if (!optionCodes.add(option.getOptionCode().trim())) {
                throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "选项编码不能重复");
            }
            int minSelect = option.getMinSelect() != null ? option.getMinSelect() : 0;
            int maxSelect = option.getMaxSelect() != null ? option.getMaxSelect() : 0;
            if (minSelect < 0 || maxSelect < minSelect) {
                throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "选项选择数量不合法");
            }
            if (option.getItems() == null || option.getItems().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(),
                        "选项「" + option.getOptionName() + "」至少配置一个候选服务");
            }
            Set<Long> serviceIds = new LinkedHashSet<>();
            for (PackageOptionItemRequest item : option.getItems()) {
                requireGroomingService(item.getServiceId());
                if (!serviceIds.add(item.getServiceId())) {
                    throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(),
                            "同一选项内服务不能重复");
                }
            }
        }
    }

    private PackageTemplateView buildTemplateView(ServicePackageTemplate template,
                                                  Map<Long, String> serviceNames) {
        List<ServicePackageTemplateItem> items = templateItemMapper.selectList(
                new LambdaQueryWrapper<ServicePackageTemplateItem>()
                        .eq(ServicePackageTemplateItem::getPackageId, template.getId())
                        .orderByAsc(ServicePackageTemplateItem::getSort)
                        .orderByAsc(ServicePackageTemplateItem::getId));
        List<PackageItemView> itemViews = new ArrayList<>();
        for (ServicePackageTemplateItem item : items) {
            itemViews.add(PackageItemView.builder()
                    .id(item.getId())
                    .serviceId(item.getServiceId())
                    .serviceName(serviceNames.get(item.getServiceId()))
                    .includedTimes(item.getIncludedTimes())
                    .extraPrice(item.getExtraPrice())
                    .selectableGroup(item.getSelectableGroup())
                    .build());
        }

        List<ServicePackageOption> options = optionMapper.selectList(
                new LambdaQueryWrapper<ServicePackageOption>()
                        .eq(ServicePackageOption::getPackageId, template.getId())
                        .orderByAsc(ServicePackageOption::getSort)
                        .orderByAsc(ServicePackageOption::getId));
        List<PackageOptionView> optionViews = new ArrayList<>();
        for (ServicePackageOption option : options) {
            List<ServicePackageOptionItem> optionItems = optionItemMapper.selectList(
                    new LambdaQueryWrapper<ServicePackageOptionItem>()
                            .eq(ServicePackageOptionItem::getOptionId, option.getId())
                            .orderByAsc(ServicePackageOptionItem::getId));
            List<PackageOptionItemView> optionItemViews = new ArrayList<>();
            for (ServicePackageOptionItem item : optionItems) {
                optionItemViews.add(PackageOptionItemView.builder()
                        .id(item.getId())
                        .serviceId(item.getServiceId())
                        .serviceName(serviceNames.get(item.getServiceId()))
                        .addPrice(item.getAddPrice())
                        .includedTimes(item.getIncludedTimes())
                        .status(item.getStatus())
                        .build());
            }
            optionViews.add(PackageOptionView.builder()
                    .id(option.getId())
                    .optionCode(option.getOptionCode())
                    .optionName(option.getOptionName())
                    .minSelect(option.getMinSelect())
                    .maxSelect(option.getMaxSelect())
                    .required(option.getRequired())
                    .items(optionItemViews)
                    .build());
        }

        return PackageTemplateView.builder()
                .id(template.getId())
                .packageCode(template.getPackageCode())
                .name(template.getName())
                .coverUrl(template.getCoverUrl())
                .description(template.getDescription())
                .salePrice(template.getSalePrice())
                .validDays(template.getValidDays())
                .status(template.getStatus())
                .allowCrossStore(template.getAllowCrossStore())
                .items(itemViews)
                .options(optionViews)
                .build();
    }

    private UserPackageView buildUserPackageView(UserServicePackage userPackage) {
        if (!isPackageActive(userPackage) && ACTIVE.equals(userPackage.getStatus())) {
            userPackage.setStatus(EXPIRED);
        }
        List<UserServicePackageItem> items = userPackageItemMapper.selectList(
                new LambdaQueryWrapper<UserServicePackageItem>()
                        .eq(UserServicePackageItem::getUserPackageId, userPackage.getId())
                        .orderByAsc(UserServicePackageItem::getId));
        Map<Long, String> serviceNames = serviceNameMap();
        List<UserPackageItemView> itemViews = new ArrayList<>();
        for (UserServicePackageItem item : items) {
            itemViews.add(UserPackageItemView.builder()
                    .id(item.getId())
                    .serviceId(item.getServiceId())
                    .serviceName(serviceNames.get(item.getServiceId()))
                    .totalTimes(item.getTotalTimes())
                    .usedTimes(item.getUsedTimes())
                    .frozenTimes(item.getFrozenTimes())
                    .remainingTimes(remainingTimes(item))
                    .serviceSnapshot(item.getServiceSnapshot())
                    .build());
        }
        return UserPackageView.builder()
                .id(userPackage.getId())
                .packageInstanceNo(userPackage.getPackageInstanceNo())
                .packageId(userPackage.getPackageId())
                .packageName(userPackage.getNameSnapshot())
                .petId(userPackage.getPetId())
                .petName(petName(userPackage.getPetId()))
                .storeId(userPackage.getStoreId())
                .storeName(storeName(userPackage.getStoreId()))
                .status(userPackage.getStatus())
                .startTime(userPackage.getStartTime())
                .expireTime(userPackage.getExpireTime())
                .selectionSnapshot(userPackage.getSelectionSnapshot())
                .items(itemViews)
                .build();
    }

    private UserServicePackage requireOwnedPackage(Long userId, Long userPackageId) {
        UserServicePackage userPackage = userPackageMapper.selectById(userPackageId);
        if (userPackage == null || !userPackage.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.USER_PACKAGE_NOT_FOUND);
        }
        return userPackage;
    }

    private boolean isPackageActive(UserServicePackage userPackage) {
        if (!ACTIVE.equals(userPackage.getStatus())) {
            return false;
        }
        if (userPackage.getExpireTime() != null
                && userPackage.getExpireTime().isBefore(LocalDateTime.now())) {
            userPackage.setStatus(EXPIRED);
            userPackage.setUpdatedAt(LocalDateTime.now());
            userPackageMapper.updateById(userPackage);
            return false;
        }
        return true;
    }

    private void markUsedUpIfNecessary(Long userPackageId) {
        List<UserServicePackageItem> items = userPackageItemMapper.selectList(
                new LambdaQueryWrapper<UserServicePackageItem>()
                        .eq(UserServicePackageItem::getUserPackageId, userPackageId));
        boolean allUsedUp = items.stream().allMatch(item -> remainingTimes(item) <= 0);
        if (allUsedUp) {
            userPackageMapper.update(null, new LambdaUpdateWrapper<UserServicePackage>()
                    .eq(UserServicePackage::getId, userPackageId)
                    .eq(UserServicePackage::getStatus, ACTIVE)
                    .set(UserServicePackage::getStatus, USED_UP)
                    .set(UserServicePackage::getUpdatedAt, LocalDateTime.now()));
        }
    }

    private int remainingTimes(UserServicePackageItem item) {
        int total = item.getTotalTimes() != null ? item.getTotalTimes() : 0;
        int used = item.getUsedTimes() != null ? item.getUsedTimes() : 0;
        int frozen = item.getFrozenTimes() != null ? item.getFrozenTimes() : 0;
        return total - used - frozen;
    }

    private ServicePackageTemplate requireEnabledTemplate(Long templateId) {
        ServicePackageTemplate template = requireTemplate(templateId);
        if (!ENABLED.equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.PACKAGE_NOT_AVAILABLE);
        }
        return template;
    }

    private ServicePackageTemplate requireTemplate(Long templateId) {
        ServicePackageTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
        }
        return template;
    }

    private GroomingService requireGroomingService(Long serviceId) {
        GroomingService service = groomingServiceMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "美容服务不存在: " + serviceId);
        }
        return service;
    }

    private void applyKeyword(LambdaQueryWrapper<ServicePackageTemplate> wrapper, String keyword) {
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(ServicePackageTemplate::getName, value)
                    .or().like(ServicePackageTemplate::getPackageCode, value));
        }
    }

    private Map<Long, String> serviceNameMap() {
        Map<Long, String> names = new HashMap<>();
        for (GroomingService service : groomingServiceMapper.selectList(null)) {
            names.put(service.getId(), service.getName());
        }
        return names;
    }

    private Map<Long, String> serviceNames() {
        return serviceNameMap();
    }

    private String petName(Long petId) {
        if (petId == null) {
            return null;
        }
        Pet pet = petService.getPetById(petId);
        return pet != null ? pet.getPetName() : null;
    }

    private String storeName(Long storeId) {
        if (storeId == null) {
            return null;
        }
        CommerceStore store = storeMapper.selectById(storeId);
        return store != null ? store.getName() : null;
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

    private String toStoreSnapshot(CommerceStore store) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", store.getId());
        snapshot.put("storeCode", store.getStoreCode());
        snapshot.put("name", store.getName());
        snapshot.put("address", store.getAddress());
        return toJson(snapshot);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return ENABLED;
        }
        String value = status.trim().toUpperCase();
        if (!ENABLED.equals(value) && !DISABLED.equals(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "状态仅支持 ENABLED/DISABLED");
        }
        return value;
    }

    private JsonNode readSnapshot(String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "套餐订单缺少快照");
        }
        try {
            return objectMapper.readTree(snapshot);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "套餐订单快照解析失败");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "套餐快照序列化失败");
        }
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

    private String generateInstanceNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "PKG" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

    private String generateUsageNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "USG" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

    private record SelectionResult(List<PackageSelectionItemView> items,
                                   List<ServicePackageOptionItem> selectedItems,
                                   List<ServicePackageOption> options) {
    }

}
