package com.itsworld.commerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.commerce.inventory.service.InventoryService;
import com.itsworld.commerce.order.dto.OrderDetailView;
import com.itsworld.commerce.order.dto.OrderItemView;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.entity.CommerceOrderDiscount;
import com.itsworld.commerce.order.entity.CommerceOrderItem;
import com.itsworld.commerce.order.entity.CommerceOrderStatusLog;
import com.itsworld.commerce.order.entity.CommercePaymentLink;
import com.itsworld.commerce.order.event.CommerceOrderStatusChangedEvent;
import com.itsworld.commerce.order.mapper.CommerceOrderDiscountMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderItemMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderStatusLogMapper;
import com.itsworld.commerce.order.mapper.CommercePaymentLinkMapper;
import com.itsworld.commerce.order.service.CommerceOrderService;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.member.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商业订单业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommerceOrderServiceImpl implements CommerceOrderService {

    private final CommerceOrderMapper orderMapper;
    private final CommerceOrderItemMapper orderItemMapper;
    private final CommerceOrderDiscountMapper orderDiscountMapper;
    private final CommerceOrderStatusLogMapper orderStatusLogMapper;
    private final CommercePaymentLinkMapper paymentLinkMapper;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<OrderDetailView> pageUserOrders(Long userId, String status,
                                                      long pageNum, long pageSize) {
        Page<CommerceOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommerceOrder> wrapper = new LambdaQueryWrapper<CommerceOrder>()
                .eq(CommerceOrder::getUserId, userId)
                .orderByDesc(CommerceOrder::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(CommerceOrder::getOrderStatus, status);
        }
        Page<CommerceOrder> result = orderMapper.selectPage(page, wrapper);
        List<OrderDetailView> records = new ArrayList<>();
        for (CommerceOrder order : result.getRecords()) {
            records.add(buildDetail(order));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public OrderDetailView getUserOrder(Long userId, String orderNo) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return buildDetail(order);
    }

    @Override
    public PageResult<OrderDetailView> pageAdminOrders(String status, String keyword,
                                                       long pageNum, long pageSize) {
        Page<CommerceOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommerceOrder> wrapper = new LambdaQueryWrapper<CommerceOrder>()
                .orderByDesc(CommerceOrder::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(CommerceOrder::getOrderStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CommerceOrder::getOrderNo, keyword.trim());
        }
        Page<CommerceOrder> result = orderMapper.selectPage(page, wrapper);
        List<OrderDetailView> records = new ArrayList<>();
        for (CommerceOrder order : result.getRecords()) {
            records.add(buildDetail(order));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public OrderDetailView getAdminOrder(String orderNo) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return buildDetail(order);
    }

    @Override
    @Transactional
    public void cancelUserOrder(Long userId, String orderNo, String reason) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (!"WAIT_PAY".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "当前订单状态不可取消");
        }
        String finalReason = StringUtils.hasText(reason) ? reason.trim() : "用户取消";
        int updated = orderMapper.cancelIfWaitPay(order.getId(), finalReason);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "订单状态已变化，请刷新");
        }
        inventoryService.releaseOrderStock(order.getId());
        if (order.getCouponId() != null) {
            couponService.releaseCoupon(userId, order.getCouponId(), orderNo);
        }
        closePendingPayments(order.getId());
        writeStatusLog(order.getId(), "WAIT_PAY", "CANCELLED", "CANCEL", "USER",
                userId, finalReason);
        eventPublisher.publishEvent(new CommerceOrderStatusChangedEvent(
                order, CommerceOrderStatusChangedEvent.ACTION_CANCELLED, "USER", userId));
        log.info("订单取消: orderNo={}, userId={}", orderNo, userId);
    }

    @Override
    @Transactional
    public void closeAdminOrder(String orderNo, String reason) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (!"WAIT_PAY".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "当前订单状态不可关闭");
        }
        String finalReason = StringUtils.hasText(reason) ? reason.trim() : "管理员关闭";
        int updated = orderMapper.closeIfWaitPay(order.getId(), finalReason);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "订单状态已变化，请刷新");
        }
        inventoryService.releaseOrderStock(order.getId());
        if (order.getCouponId() != null) {
            couponService.releaseCoupon(order.getUserId(), order.getCouponId(), orderNo);
        }
        closePendingPayments(order.getId());
        writeStatusLog(order.getId(), "WAIT_PAY", "CLOSED", "CLOSE", "ADMIN",
                null, finalReason);
        eventPublisher.publishEvent(new CommerceOrderStatusChangedEvent(
                order, CommerceOrderStatusChangedEvent.ACTION_CLOSED, "ADMIN", null));
        log.info("订单关闭: orderNo={}", orderNo);
    }

    @Override
    @Transactional
    public void markPaid(Long orderId, String operatorType, Long operatorId) {
        CommerceOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if ("PAID".equals(order.getOrderStatus())) {
            return;
        }
        int updated = orderMapper.markPaid(orderId, order.getPayableAmount());
        if (updated != 1) {
            CommerceOrder latest = orderMapper.selectById(orderId);
            if (latest != null && "PAID".equals(latest.getOrderStatus())) {
                return;
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "订单支付状态更新失败");
        }
        inventoryService.confirmOrderStock(orderId);
        if (order.getCouponId() != null) {
            couponService.consumeCoupon(order.getUserId(), order.getCouponId(), order.getOrderNo());
        }
        writeStatusLog(orderId, "WAIT_PAY", "PAID", "PAY", operatorType, operatorId, "支付成功");
        eventPublisher.publishEvent(new CommerceOrderStatusChangedEvent(
                order, CommerceOrderStatusChangedEvent.ACTION_PAID, operatorType, operatorId));
        log.info("订单支付成功: orderNo={}, orderId={}", order.getOrderNo(), orderId);
    }

    @Override
    @Transactional
    public void closeExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<CommerceOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<CommerceOrder>()
                .eq(CommerceOrder::getOrderStatus, "WAIT_PAY")
                .lt(CommerceOrder::getExpireTime, now)
                .orderByAsc(CommerceOrder::getId)
                .last("LIMIT 100"));
        for (CommerceOrder order : orders) {
            int updated = orderMapper.closeIfWaitPay(order.getId(), "支付超时关闭");
            if (updated != 1) {
                continue;
            }
            inventoryService.releaseOrderStock(order.getId());
            if (order.getCouponId() != null) {
                couponService.releaseCoupon(order.getUserId(), order.getCouponId(),
                        order.getOrderNo());
            }
            closePendingPayments(order.getId());
            writeStatusLog(order.getId(), "WAIT_PAY", "CLOSED", "EXPIRE", "SYSTEM",
                    null, "支付超时关闭");
            eventPublisher.publishEvent(new CommerceOrderStatusChangedEvent(
                    order, CommerceOrderStatusChangedEvent.ACTION_CLOSED, "SYSTEM", null));
            log.info("订单超时关闭: orderNo={}", order.getOrderNo());
        }
    }

    private OrderDetailView buildDetail(CommerceOrder order) {
        List<CommerceOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<CommerceOrderItem>()
                        .eq(CommerceOrderItem::getOrderId, order.getId())
                        .orderByAsc(CommerceOrderItem::getId));
        List<OrderItemView> itemViews = new ArrayList<>();
        for (CommerceOrderItem item : items) {
            itemViews.add(OrderItemView.builder()
                    .id(item.getId())
                    .itemType(item.getItemType())
                    .targetId(item.getTargetId())
                    .skuId(item.getSkuId())
                    .quantity(item.getQuantity())
                    .titleSnapshot(item.getTitleSnapshot())
                    .specSnapshot(item.getSpecSnapshot())
                    .unitPrice(item.getUnitPrice())
                    .originalTotal(item.getOriginalTotal())
                    .discountAllocated(item.getDiscountAllocated())
                    .payableTotal(item.getPayableTotal())
                    .fulfilledQty(item.getFulfilledQty())
                    .refundedAmount(item.getRefundedAmount())
                    .build());
        }
        List<CommerceOrderDiscount> discounts = orderDiscountMapper.selectList(
                new LambdaQueryWrapper<CommerceOrderDiscount>()
                        .eq(CommerceOrderDiscount::getOrderId, order.getId())
                        .orderByAsc(CommerceOrderDiscount::getId));
        CommercePaymentLink payment = paymentLinkMapper.selectLatestByOrderId(order.getId());
        return OrderDetailView.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .orderType(order.getOrderType())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .fulfillmentStatus(order.getFulfillmentStatus())
                .originalAmount(order.getOriginalAmount())
                .discountAmount(order.getDiscountAmount())
                .payableAmount(order.getPayableAmount())
                .paidAmount(order.getPaidAmount())
                .refundedAmount(order.getRefundedAmount())
                .couponId(order.getCouponId())
                .addressSnapshot(order.getAddressSnapshot())
                .storeSnapshot(order.getStoreSnapshot())
                .expireTime(order.getExpireTime())
                .paidTime(order.getPaidTime())
                .closeTime(order.getCloseTime())
                .cancelReason(order.getCancelReason())
                .remark(order.getRemark())
                .createdAt(order.getCreatedAt())
                .items(itemViews)
                .discounts(discounts)
                .latestPayment(payment)
                .build();
    }

    private void closePendingPayments(Long orderId) {
        paymentLinkMapper.update(null, new LambdaUpdateWrapper<CommercePaymentLink>()
                .eq(CommercePaymentLink::getOrderId, orderId)
                .eq(CommercePaymentLink::getStatus, "CREATED")
                .set(CommercePaymentLink::getStatus, "CLOSED")
                .set(CommercePaymentLink::getUpdatedAt, LocalDateTime.now()));
    }

    private void writeStatusLog(Long orderId, String from, String to, String action,
                                String operatorType, Long operatorId, String remark) {
        orderStatusLogMapper.insert(CommerceOrderStatusLog.builder()
                .orderId(orderId)
                .fromStatus(from)
                .toStatus(to)
                .action(action)
                .operatorType(operatorType)
                .operatorId(operatorId)
                .remark(remark)
                .createdAt(LocalDateTime.now())
                .build());
    }

}
