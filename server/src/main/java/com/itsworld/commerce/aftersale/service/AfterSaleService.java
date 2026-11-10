package com.itsworld.commerce.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsworld.commerce.aftersale.dto.AfterSaleApplyItemRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleApplyRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleItemView;
import com.itsworld.commerce.aftersale.dto.AfterSaleReviewRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleView;
import com.itsworld.commerce.aftersale.entity.CommerceAfterSale;
import com.itsworld.commerce.aftersale.entity.CommerceAfterSaleItem;
import com.itsworld.commerce.aftersale.entity.CommerceAfterSaleLog;
import com.itsworld.commerce.aftersale.entity.CommerceRefundLink;
import com.itsworld.commerce.aftersale.mapper.CommerceAfterSaleItemMapper;
import com.itsworld.commerce.aftersale.mapper.CommerceAfterSaleLogMapper;
import com.itsworld.commerce.aftersale.mapper.CommerceAfterSaleMapper;
import com.itsworld.commerce.aftersale.mapper.CommerceRefundLinkMapper;
import com.itsworld.commerce.common.CommerceRefundUtils;
import com.itsworld.commerce.fulfillment.entity.CommerceShipment;
import com.itsworld.commerce.fulfillment.mapper.CommerceShipmentMapper;
import com.itsworld.commerce.inventory.service.InventoryService;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.entity.CommerceOrderItem;
import com.itsworld.commerce.order.entity.CommercePaymentLink;
import com.itsworld.commerce.order.mapper.CommerceOrderItemMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.order.mapper.CommercePaymentLinkMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.user.entity.User;
import com.itsworld.user.service.UserService;
import com.itsworld.wallet.dto.WalletRefundResult;
import com.itsworld.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 售后服务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleService {

    private static final String TYPE_REFUND_ONLY = "REFUND_ONLY";
    private static final String TYPE_RETURN_REFUND = "RETURN_REFUND";
    private static final String ORDER_TYPE_PRODUCT = "PRODUCT";
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            "APPLIED", "REVIEWING", "APPROVED", "REFUNDING", "REFUNDED");
    private static final Set<String> CAN_APPLY_ORDER_STATUSES = Set.of(
            "PAID", "PROCESSING", "SHIPPED", "COMPLETED");

    private final CommerceAfterSaleMapper afterSaleMapper;
    private final CommerceAfterSaleItemMapper afterSaleItemMapper;
    private final CommerceAfterSaleLogMapper afterSaleLogMapper;
    private final CommerceRefundLinkMapper refundLinkMapper;
    private final CommerceShipmentMapper shipmentMapper;
    private final CommerceOrderMapper orderMapper;
    private final CommerceOrderItemMapper orderItemMapper;
    private final CommercePaymentLinkMapper paymentLinkMapper;
    private final InventoryService inventoryService;
    private final WalletService walletService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AfterSaleView apply(Long userId, AfterSaleApplyRequest request) {
        String type = request.getType().trim().toUpperCase();
        if (!TYPE_REFUND_ONLY.equals(type) && !TYPE_RETURN_REFUND.equals(type)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(),
                    "售后类型仅支持 REFUND_ONLY/RETURN_REFUND");
        }
        CommerceOrder order = orderMapper.selectByOrderNo(request.getOrderNo());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (!CAN_APPLY_ORDER_STATUSES.contains(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_ALLOWED);
        }
        if (nvl(order.getPaidAmount()) <= nvl(order.getRefundedAmount())) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_ALLOWED.getCode(), "订单已无可退金额");
        }
        CommerceShipment shipment = shipmentMapper.selectLatestByOrderId(order.getId());
        boolean shipped = shipment != null
                && ("SHIPPED".equals(shipment.getStatus()) || "DELIVERED".equals(shipment.getStatus()));
        if (TYPE_REFUND_ONLY.equals(type) && shipped) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_ALLOWED.getCode(),
                    "已发货订单请选择退货退款");
        }
        if (TYPE_RETURN_REFUND.equals(type) && !shipped) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_ALLOWED.getCode(),
                    "未发货订单请选择仅退款");
        }

        long totalAmount = 0L;
        List<CommerceAfterSaleItem> afterSaleItems = new ArrayList<>();
        for (AfterSaleApplyItemRequest requestItem : request.getItems()) {
            CommerceOrderItem orderItem = orderItemMapper.selectById(requestItem.getOrderItemId());
            if (orderItem == null || !orderItem.getOrderId().equals(order.getId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单项不存在");
            }
            if (!ORDER_TYPE_PRODUCT.equals(orderItem.getItemType())) {
                throw new BusinessException(ErrorCode.AFTER_SALE_NOT_ALLOWED.getCode(),
                        "仅支持商品订单售后");
            }
            int quantity = orderItem.getQuantity() != null ? orderItem.getQuantity() : 0;
            if (requestItem.getQty() > quantity) {
                throw new BusinessException(ErrorCode.AFTER_SALE_AMOUNT_INVALID.getCode(),
                        "售后数量不能超过购买数量");
            }
            if (hasActiveAfterSale(orderItem.getId())) {
                throw new BusinessException(ErrorCode.AFTER_SALE_DUPLICATE);
            }
            long amount = resolveAmount(orderItem, requestItem.getQty());
            if (amount <= 0) {
                throw new BusinessException(ErrorCode.AFTER_SALE_AMOUNT_INVALID.getCode(),
                        "订单项已无可退金额");
            }
            totalAmount += amount;
            afterSaleItems.add(CommerceAfterSaleItem.builder()
                    .orderItemId(orderItem.getId())
                    .skuId(orderItem.getSkuId())
                    .qty(requestItem.getQty())
                    .amount(amount)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        if (totalAmount <= 0) {
            throw new BusinessException(ErrorCode.AFTER_SALE_AMOUNT_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        CommerceAfterSale afterSale = CommerceAfterSale.builder()
                .afterSaleNo(generateAfterSaleNo())
                .orderId(order.getId())
                .userId(userId)
                .type(type)
                .reason(request.getReason().trim())
                .description(request.getDescription())
                .evidenceUrls(toJson(request.getEvidenceUrls()))
                .amount(totalAmount)
                .status("APPLIED")
                .createdAt(now)
                .updatedAt(now)
                .build();
        afterSaleMapper.insert(afterSale);
        for (CommerceAfterSaleItem item : afterSaleItems) {
            item.setAfterSaleId(afterSale.getId());
            afterSaleItemMapper.insert(item);
        }
        writeLog(afterSale.getId(), null, "APPLIED", "APPLY", "USER", userId,
                "提交售后申请");
        log.info("售后申请: afterSaleNo={}, orderNo={}, amount={}",
                afterSale.getAfterSaleNo(), order.getOrderNo(), totalAmount);
        return buildView(afterSale, true);
    }

    public PageResult<AfterSaleView> pageUserAfterSales(Long userId, String status,
                                                        long pageNum, long pageSize) {
        Page<CommerceAfterSale> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommerceAfterSale> wrapper = new LambdaQueryWrapper<CommerceAfterSale>()
                .eq(CommerceAfterSale::getUserId, userId)
                .orderByDesc(CommerceAfterSale::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(CommerceAfterSale::getStatus, status.trim().toUpperCase());
        }
        Page<CommerceAfterSale> result = afterSaleMapper.selectPage(page, wrapper);
        List<AfterSaleView> records = new ArrayList<>();
        for (CommerceAfterSale afterSale : result.getRecords()) {
            records.add(buildView(afterSale, false));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AfterSaleView getUserAfterSale(Long userId, String afterSaleNo) {
        CommerceAfterSale afterSale = requireAfterSale(afterSaleNo);
        if (!afterSale.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_FOUND);
        }
        return buildView(afterSale, true);
    }

    public PageResult<AfterSaleView> pageAdminAfterSales(String status, String keyword,
                                                         long pageNum, long pageSize) {
        Page<CommerceAfterSale> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommerceAfterSale> wrapper = new LambdaQueryWrapper<CommerceAfterSale>()
                .orderByDesc(CommerceAfterSale::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(CommerceAfterSale::getStatus, status.trim().toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CommerceAfterSale::getAfterSaleNo, keyword.trim());
        }
        Page<CommerceAfterSale> result = afterSaleMapper.selectPage(page, wrapper);
        List<AfterSaleView> records = new ArrayList<>();
        for (CommerceAfterSale afterSale : result.getRecords()) {
            records.add(buildView(afterSale, false));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public AfterSaleView getAdminAfterSale(String afterSaleNo) {
        return buildView(requireAfterSale(afterSaleNo), true);
    }

    @Transactional
    public AfterSaleView review(String afterSaleNo, AfterSaleReviewRequest request, Long adminId) {
        CommerceAfterSale afterSale = requireAfterSale(afterSaleNo);
        if (!"APPLIED".equals(afterSale.getStatus()) && !"REVIEWING".equals(afterSale.getStatus())) {
            throw new BusinessException(ErrorCode.AFTER_SALE_STATUS_ERROR);
        }
        String fromStatus = afterSale.getStatus();
        String toStatus = Boolean.TRUE.equals(request.getApproved()) ? "APPROVED" : "REJECTED";
        if (afterSaleMapper.review(afterSale.getId(), fromStatus, toStatus,
                request.getRemark(), adminId) != 1) {
            throw new BusinessException(ErrorCode.AFTER_SALE_STATUS_ERROR);
        }
        writeLog(afterSale.getId(), fromStatus, toStatus,
                Boolean.TRUE.equals(request.getApproved()) ? "APPROVE" : "REJECT",
                "ADMIN", adminId, request.getRemark());
        return getAdminAfterSale(afterSaleNo);
    }

    @Transactional
    public AfterSaleView executeRefund(String afterSaleNo, Long adminId) {
        CommerceAfterSale afterSale = requireAfterSale(afterSaleNo);
        if ("REFUNDED".equals(afterSale.getStatus())) {
            return buildView(afterSale, true);
        }
        if (!"APPROVED".equals(afterSale.getStatus())
                && !"REFUND_FAILED".equals(afterSale.getStatus())) {
            throw new BusinessException(ErrorCode.AFTER_SALE_STATUS_ERROR);
        }
        String fromStatus = afterSale.getStatus();
        if (afterSaleMapper.update(null, new LambdaUpdateWrapper<CommerceAfterSale>()
                .eq(CommerceAfterSale::getId, afterSale.getId())
                .in(CommerceAfterSale::getStatus, "APPROVED", "REFUND_FAILED")
                .set(CommerceAfterSale::getStatus, "REFUNDING")
                .set(CommerceAfterSale::getUpdatedAt, LocalDateTime.now())) != 1) {
            throw new BusinessException(ErrorCode.AFTER_SALE_STATUS_ERROR);
        }
        writeLog(afterSale.getId(), fromStatus, "REFUNDING", "START_REFUND",
                "ADMIN", adminId, "开始执行退款");

        CommerceOrder order = orderMapper.selectById(afterSale.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        CommercePaymentLink paymentLink = paymentLinkMapper.selectLatestPaidByOrderId(order.getId());
        if (paymentLink == null) {
            throw new BusinessException(ErrorCode.REFUND_EXECUTE_FAILED.getCode(), "未找到已支付记录");
        }
        String refundNo = generateRefundNo();
        CommerceRefundLink refundLink = CommerceRefundLink.builder()
                .refundNo(refundNo)
                .afterSaleId(afterSale.getId())
                .paymentLinkId(paymentLink.getId())
                .channel(paymentLink.getPayChannel())
                .amount(afterSale.getAmount())
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        refundLinkMapper.insert(refundLink);

        if ("WALLET".equals(paymentLink.getPayChannel())) {
            WalletRefundResult result = walletService.refundBusinessPayment(
                    afterSale.getUserId(), paymentLink.getId(), order.getOrderNo(),
                    afterSale.getAmount(), afterSale.getReason(), refundNo);
            refundLink.setExternalRefundNo(result.getRefundNo());
        } else if ("MOCK_WECHAT".equals(paymentLink.getPayChannel())) {
            refundLink.setExternalRefundNo("MOCK-" + refundNo);
        } else {
            throw new BusinessException(ErrorCode.REFUND_EXECUTE_FAILED.getCode(),
                    "真实微信退款待接入商户资质与证书");
        }
        refundLink.setStatus("REFUNDED");
        refundLink.setUpdatedAt(LocalDateTime.now());
        refundLinkMapper.updateById(refundLink);

        for (CommerceAfterSaleItem afterSaleItem : listAfterSaleItems(afterSale.getId())) {
            CommerceOrderItem orderItem = orderItemMapper.selectById(afterSaleItem.getOrderItemId());
            if (orderItem == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单项不存在");
            }
            inventoryService.restoreRefundedStock(order.getId(), orderItem.getId(),
                    orderItem.getSkuId(), afterSaleItem.getQty(),
                    "售后退款：" + afterSale.getAfterSaleNo());
            if (orderItemMapper.addRefundedAmount(orderItem.getId(), afterSaleItem.getAmount()) != 1) {
                throw new BusinessException(ErrorCode.REFUND_EXECUTE_FAILED.getCode(),
                        "订单项退款金额更新失败");
            }
        }

        long newRefunded = nvl(order.getRefundedAmount()) + afterSale.getAmount();
        String paymentStatus = newRefunded >= nvl(order.getPaidAmount())
                ? "REFUNDED" : "PARTIAL_REFUND";
        if (orderMapper.addRefundedAmount(order.getId(), afterSale.getAmount(), paymentStatus) != 1) {
            throw new BusinessException(ErrorCode.REFUND_EXECUTE_FAILED.getCode(),
                    "订单退款金额更新失败");
        }

        afterSaleMapper.updateRefundStatus(afterSale.getId(), "REFUNDED", LocalDateTime.now());
        writeLog(afterSale.getId(), "REFUNDING", "REFUNDED", "REFUND",
                "ADMIN", adminId, "退款成功：" + refundNo);
        log.info("售后退款完成: afterSaleNo={}, orderNo={}, amount={}, channel={}",
                afterSaleNo, order.getOrderNo(), afterSale.getAmount(), paymentLink.getPayChannel());
        return getAdminAfterSale(afterSaleNo);
    }

    private long resolveAmount(CommerceOrderItem orderItem, int qty) {
        return CommerceRefundUtils.resolveRefundAmount(
                nvl(orderItem.getPayableTotal()), nvl(orderItem.getRefundedAmount()),
                orderItem.getQuantity() != null ? orderItem.getQuantity() : 0, qty);
    }

    private boolean hasActiveAfterSale(Long orderItemId) {
        List<CommerceAfterSaleItem> items = afterSaleItemMapper.selectList(
                new LambdaQueryWrapper<CommerceAfterSaleItem>()
                        .eq(CommerceAfterSaleItem::getOrderItemId, orderItemId));
        for (CommerceAfterSaleItem item : items) {
            CommerceAfterSale afterSale = afterSaleMapper.selectById(item.getAfterSaleId());
            if (afterSale != null && ACTIVE_STATUSES.contains(afterSale.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private List<CommerceAfterSaleItem> listAfterSaleItems(Long afterSaleId) {
        return afterSaleItemMapper.selectList(new LambdaQueryWrapper<CommerceAfterSaleItem>()
                .eq(CommerceAfterSaleItem::getAfterSaleId, afterSaleId)
                .orderByAsc(CommerceAfterSaleItem::getId));
    }

    private CommerceAfterSale requireAfterSale(String afterSaleNo) {
        CommerceAfterSale afterSale = afterSaleMapper.selectByAfterSaleNo(afterSaleNo);
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_FOUND);
        }
        return afterSale;
    }

    private AfterSaleView buildView(CommerceAfterSale afterSale, boolean withDetail) {
        CommerceOrder order = orderMapper.selectById(afterSale.getOrderId());
        List<AfterSaleItemView> itemViews = new ArrayList<>();
        for (CommerceAfterSaleItem item : listAfterSaleItems(afterSale.getId())) {
            CommerceOrderItem orderItem = orderItemMapper.selectById(item.getOrderItemId());
            itemViews.add(AfterSaleItemView.builder()
                    .id(item.getId())
                    .orderItemId(item.getOrderItemId())
                    .skuId(item.getSkuId())
                    .titleSnapshot(orderItem != null ? orderItem.getTitleSnapshot() : null)
                    .qty(item.getQty())
                    .amount(item.getAmount())
                    .build());
        }
        User user = userService.getUserById(afterSale.getUserId());
        List<CommerceAfterSaleLog> logs = null;
        if (withDetail) {
            logs = afterSaleLogMapper.selectList(new LambdaQueryWrapper<CommerceAfterSaleLog>()
                    .eq(CommerceAfterSaleLog::getAfterSaleId, afterSale.getId())
                    .orderByAsc(CommerceAfterSaleLog::getId));
        }
        return AfterSaleView.builder()
                .id(afterSale.getId())
                .afterSaleNo(afterSale.getAfterSaleNo())
                .orderId(afterSale.getOrderId())
                .orderNo(order != null ? order.getOrderNo() : null)
                .userId(afterSale.getUserId())
                .username(user != null ? user.getUsername() : null)
                .type(afterSale.getType())
                .reason(afterSale.getReason())
                .description(afterSale.getDescription())
                .evidenceUrls(parseEvidence(afterSale.getEvidenceUrls()))
                .amount(afterSale.getAmount())
                .status(afterSale.getStatus())
                .reviewRemark(afterSale.getReviewRemark())
                .reviewedBy(afterSale.getReviewedBy())
                .reviewedAt(afterSale.getReviewedAt())
                .refundedAt(afterSale.getRefundedAt())
                .createdAt(afterSale.getCreatedAt())
                .items(itemViews)
                .logs(logs)
                .refund(withDetail ? refundLinkMapper.selectLatestByAfterSaleId(afterSale.getId()) : null)
                .build();
    }

    private void writeLog(Long afterSaleId, String fromStatus, String toStatus, String action,
                          String operatorType, Long operatorId, String remark) {
        afterSaleLogMapper.insert(CommerceAfterSaleLog.builder()
                .afterSaleId(afterSaleId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .action(action)
                .operatorType(operatorType)
                .operatorId(operatorId)
                .remark(remark)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private List<String> parseEvidence(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("售后凭证解析失败: {}", value);
            return List.of();
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "售后凭证序列化失败");
        }
    }

    private long nvl(Long value) {
        return value != null ? value : 0L;
    }

    private String generateAfterSaleNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "AS" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

    private String generateRefundNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "RF" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

}
