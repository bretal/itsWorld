package com.itsworld.commerce.fulfillment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.commerce.fulfillment.dto.ShipmentCreateRequest;
import com.itsworld.commerce.fulfillment.dto.ShipmentItemView;
import com.itsworld.commerce.fulfillment.dto.ShipmentView;
import com.itsworld.commerce.fulfillment.entity.CommerceShipment;
import com.itsworld.commerce.fulfillment.entity.CommerceShipmentItem;
import com.itsworld.commerce.fulfillment.mapper.CommerceShipmentItemMapper;
import com.itsworld.commerce.fulfillment.mapper.CommerceShipmentMapper;
import com.itsworld.commerce.fulfillment.service.ShipmentService;
import com.itsworld.commerce.order.entity.CommerceOrder;
import com.itsworld.commerce.order.entity.CommerceOrderItem;
import com.itsworld.commerce.order.entity.CommerceOrderStatusLog;
import com.itsworld.commerce.order.mapper.CommerceOrderItemMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderMapper;
import com.itsworld.commerce.order.mapper.CommerceOrderStatusLogMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 物流发货业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private static final String ORDER_TYPE_PRODUCT = "PRODUCT";
    private static final String STATUS_SHIPPED = "SHIPPED";
    private static final String STATUS_DELIVERED = "DELIVERED";
    private static final int AUTO_CONFIRM_DAYS = 7;

    private final CommerceShipmentMapper shipmentMapper;
    private final CommerceShipmentItemMapper shipmentItemMapper;
    private final CommerceOrderMapper orderMapper;
    private final CommerceOrderItemMapper orderItemMapper;
    private final CommerceOrderStatusLogMapper orderStatusLogMapper;

    @Override
    @Transactional
    public ShipmentView shipOrder(String orderNo, ShipmentCreateRequest request, Long operatorId) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        if (!ORDER_TYPE_PRODUCT.equals(order.getOrderType())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_SHIPPABLE.getCode(),
                    "仅商品订单支持发货");
        }
        CommerceShipment existing = shipmentMapper.selectLatestByOrderId(order.getId());
        if (existing != null) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_SHIPPED);
        }
        if (!"PAID".equals(order.getOrderStatus())
                && !"PROCESSING".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_SHIPPABLE);
        }
        List<CommerceOrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<CommerceOrderItem>()
                        .eq(CommerceOrderItem::getOrderId, order.getId())
                        .eq(CommerceOrderItem::getItemType, ORDER_TYPE_PRODUCT)
                        .orderByAsc(CommerceOrderItem::getId));
        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_SHIPPABLE.getCode(), "订单没有可发货商品");
        }

        LocalDateTime now = LocalDateTime.now();
        CommerceShipment shipment = CommerceShipment.builder()
                .shipmentNo(generateShipmentNo())
                .orderId(order.getId())
                .deliveryCompany(request.getDeliveryCompany().trim())
                .trackingNo(request.getTrackingNo().trim())
                .status(STATUS_SHIPPED)
                .shippedTime(now)
                .remark(request.getRemark())
                .createdAt(now)
                .updatedAt(now)
                .build();
        shipmentMapper.insert(shipment);

        for (CommerceOrderItem orderItem : orderItems) {
            int fulfilled = orderItem.getFulfilledQty() != null ? orderItem.getFulfilledQty() : 0;
            int quantity = orderItem.getQuantity() != null ? orderItem.getQuantity() : 0;
            int qty = quantity - fulfilled;
            if (qty <= 0) {
                continue;
            }
            shipmentItemMapper.insert(CommerceShipmentItem.builder()
                    .shipmentId(shipment.getId())
                    .orderItemId(orderItem.getId())
                    .skuId(orderItem.getSkuId())
                    .qty(qty)
                    .createdAt(now)
                    .build());
            if (orderItemMapper.updateFulfilledQty(orderItem.getId(), quantity) != 1) {
                throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "订单项发货数量更新失败");
            }
        }
        if (orderMapper.shipIfPaid(order.getId()) != 1) {
            throw new BusinessException(ErrorCode.ORDER_NOT_SHIPPABLE);
        }
        orderStatusLogMapper.insert(CommerceOrderStatusLog.builder()
                .orderId(order.getId())
                .fromStatus(order.getOrderStatus())
                .toStatus(STATUS_SHIPPED)
                .action("SHIP")
                .operatorType("ADMIN")
                .operatorId(operatorId)
                .remark("订单发货：" + shipment.getShipmentNo())
                .createdAt(now)
                .build());
        log.info("订单发货: orderNo={}, shipmentNo={}", orderNo, shipment.getShipmentNo());
        return buildView(shipment);
    }

    @Override
    public ShipmentView getUserShipment(Long userId, String orderNo) {
        CommerceOrder order = requireOwnedOrder(userId, orderNo);
        CommerceShipment shipment = shipmentMapper.selectLatestByOrderId(order.getId());
        if (shipment == null) {
            throw new BusinessException(ErrorCode.SHIPMENT_NOT_FOUND);
        }
        return buildView(shipment);
    }

    @Override
    public ShipmentView getAdminShipment(String orderNo) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        CommerceShipment shipment = shipmentMapper.selectLatestByOrderId(order.getId());
        if (shipment == null) {
            throw new BusinessException(ErrorCode.SHIPMENT_NOT_FOUND);
        }
        return buildView(shipment);
    }

    @Override
    public PageResult<ShipmentView> pageAdminShipments(String status, String keyword,
                                                       long pageNum, long pageSize) {
        Page<CommerceShipment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommerceShipment> wrapper = new LambdaQueryWrapper<CommerceShipment>()
                .orderByDesc(CommerceShipment::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(CommerceShipment::getStatus, status.trim().toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(CommerceShipment::getShipmentNo, value)
                    .or().like(CommerceShipment::getTrackingNo, value));
        }
        Page<CommerceShipment> result = shipmentMapper.selectPage(page, wrapper);
        List<ShipmentView> records = new ArrayList<>();
        for (CommerceShipment shipment : result.getRecords()) {
            records.add(buildView(shipment));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void confirmReceipt(Long userId, String orderNo) {
        CommerceOrder order = requireOwnedOrder(userId, orderNo);
        CommerceShipment shipment = shipmentMapper.selectLatestByOrderId(order.getId());
        if (shipment == null) {
            throw new BusinessException(ErrorCode.SHIPMENT_NOT_FOUND);
        }
        if (!STATUS_SHIPPED.equals(shipment.getStatus())) {
            throw new BusinessException(ErrorCode.SHIPMENT_NOT_RECEIVABLE);
        }
        if (shipmentMapper.markDelivered(shipment.getId()) != 1) {
            throw new BusinessException(ErrorCode.SHIPMENT_NOT_RECEIVABLE);
        }
        if (orderMapper.completeIfShipped(order.getId()) != 1) {
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "订单完成状态更新失败");
        }
        orderStatusLogMapper.insert(CommerceOrderStatusLog.builder()
                .orderId(order.getId())
                .fromStatus("SHIPPED")
                .toStatus("COMPLETED")
                .action("RECEIVE")
                .operatorType("USER")
                .operatorId(userId)
                .remark("用户确认收货")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void autoConfirmReceipts() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(AUTO_CONFIRM_DAYS);
        List<CommerceShipment> shipments = shipmentMapper.selectAutoConfirmable(deadline);
        for (CommerceShipment shipment : shipments) {
            if (shipmentMapper.markDelivered(shipment.getId()) != 1) {
                continue;
            }
            CommerceOrder order = orderMapper.selectById(shipment.getOrderId());
            if (order == null) {
                continue;
            }
            orderMapper.completeIfShipped(order.getId());
            orderStatusLogMapper.insert(CommerceOrderStatusLog.builder()
                    .orderId(order.getId())
                    .fromStatus("SHIPPED")
                    .toStatus("COMPLETED")
                    .action("AUTO_RECEIVE")
                    .operatorType("SYSTEM")
                    .operatorId(null)
                    .remark("发货后 7 天自动确认收货")
                    .createdAt(LocalDateTime.now())
                    .build());
            log.info("自动确认收货: orderNo={}, shipmentNo={}",
                    order.getOrderNo(), shipment.getShipmentNo());
        }
    }

    private CommerceOrder requireOwnedOrder(Long userId, String orderNo) {
        CommerceOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return order;
    }

    private ShipmentView buildView(CommerceShipment shipment) {
        CommerceOrder order = orderMapper.selectById(shipment.getOrderId());
        List<CommerceShipmentItem> items = shipmentItemMapper.selectList(
                new LambdaQueryWrapper<CommerceShipmentItem>()
                        .eq(CommerceShipmentItem::getShipmentId, shipment.getId())
                        .orderByAsc(CommerceShipmentItem::getId));
        List<ShipmentItemView> itemViews = new ArrayList<>();
        for (CommerceShipmentItem item : items) {
            CommerceOrderItem orderItem = orderItemMapper.selectById(item.getOrderItemId());
            itemViews.add(ShipmentItemView.builder()
                    .orderItemId(item.getOrderItemId())
                    .skuId(item.getSkuId())
                    .titleSnapshot(orderItem != null ? orderItem.getTitleSnapshot() : null)
                    .qty(item.getQty())
                    .build());
        }
        return ShipmentView.builder()
                .id(shipment.getId())
                .shipmentNo(shipment.getShipmentNo())
                .orderId(shipment.getOrderId())
                .orderNo(order != null ? order.getOrderNo() : null)
                .deliveryCompany(shipment.getDeliveryCompany())
                .trackingNo(shipment.getTrackingNo())
                .status(shipment.getStatus())
                .shippedTime(shipment.getShippedTime())
                .receivedTime(shipment.getReceivedTime())
                .remark(shipment.getRemark())
                .items(itemViews)
                .build();
    }

    private String generateShipmentNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "SH" + time + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

}
