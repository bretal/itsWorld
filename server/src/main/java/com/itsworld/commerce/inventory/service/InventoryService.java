package com.itsworld.commerce.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.commerce.catalog.entity.ShopSku;
import com.itsworld.commerce.catalog.mapper.ShopSkuMapper;
import com.itsworld.commerce.inventory.dto.InventoryAdjustRequest;
import com.itsworld.commerce.inventory.dto.InventoryLogView;
import com.itsworld.commerce.inventory.dto.InventoryView;
import com.itsworld.commerce.inventory.entity.ShopInventory;
import com.itsworld.commerce.inventory.entity.ShopInventoryLog;
import com.itsworld.commerce.inventory.entity.ShopInventoryReservation;
import com.itsworld.commerce.inventory.mapper.ShopInventoryLogMapper;
import com.itsworld.commerce.inventory.mapper.ShopInventoryMapper;
import com.itsworld.commerce.inventory.mapper.ShopInventoryReservationMapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 库存业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final long DEFAULT_WAREHOUSE_ID = 0L;

    private final ShopInventoryMapper inventoryMapper;
    private final ShopInventoryLogMapper inventoryLogMapper;
    private final ShopInventoryReservationMapper reservationMapper;
    private final ShopSkuMapper skuMapper;

    public PageResult<InventoryView> pageInventory(long pageNum, long pageSize, String keyword) {
        Page<InventoryView> page = new Page<>(pageNum, pageSize);
        Page<InventoryView> result = inventoryMapper.selectViewPage(page, keyword);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public InventoryView adjustStock(Long operatorId, InventoryAdjustRequest request) {
        if (request.getChangeQty() == null || request.getChangeQty() == 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "调整数量不能为0");
        }
        ShopSku sku = skuMapper.selectById(request.getSkuId());
        if (sku == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "SKU不存在");
        }

        ShopInventory existing = inventoryMapper.selectBySkuIdAndWarehouse(
                request.getSkuId(), DEFAULT_WAREHOUSE_ID);
        if (existing == null) {
            ShopInventory created = ShopInventory.builder()
                    .skuId(request.getSkuId())
                    .warehouseId(DEFAULT_WAREHOUSE_ID)
                    .availableQty(0)
                    .lockedQty(0)
                    .soldQty(0)
                    .version(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            try {
                inventoryMapper.insert(created);
            } catch (DuplicateKeyException e) {
                log.info("库存已被并发创建: skuId={}", request.getSkuId());
            }
        }

        ShopInventory inventory = inventoryMapper.selectBySkuIdAndWarehouseForUpdate(
                request.getSkuId(), DEFAULT_WAREHOUSE_ID);
        if (inventory == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "库存记录不存在");
        }
        int before = inventory.getAvailableQty() != null ? inventory.getAvailableQty() : 0;
        int after = before + request.getChangeQty();
        if (after < 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "可售库存不能为负数");
        }

        inventory.setAvailableQty(after);
        inventory.setVersion(inventory.getVersion() != null ? inventory.getVersion() + 1 : 1);
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryMapper.updateById(inventory);

        ShopInventoryLog logEntry = ShopInventoryLog.builder()
                .skuId(request.getSkuId())
                .bizType("ADMIN_ADJUST")
                .bizId(null)
                .changeAvailable(request.getChangeQty())
                .changeLocked(0)
                .beforeAvailable(before)
                .afterAvailable(after)
                .idempotencyKey("ADMIN_ADJUST:" + UUID.randomUUID())
                .remark("管理员#" + operatorId + " 调整库存: " + request.getReason().trim())
                .createdAt(LocalDateTime.now())
                .build();
        inventoryLogMapper.insert(logEntry);

        log.info("库存调整: skuId={}, before={}, change={}, after={}, operatorId={}",
                request.getSkuId(), before, request.getChangeQty(), after, operatorId);
        return inventoryMapper.selectViewBySkuId(request.getSkuId());
    }

    public PageResult<InventoryLogView> pageLogs(Long skuId, long pageNum, long pageSize) {
        Page<InventoryLogView> page = new Page<>(pageNum, pageSize);
        Page<InventoryLogView> result = inventoryLogMapper.selectViewPage(page, skuId);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void reserveStock(Long orderId, Long orderItemId, Long skuId, int qty,
                             java.time.LocalDateTime expireTime) {
        ShopInventory inventory = inventoryMapper.selectBySkuIdAndWarehouseForUpdate(
                skuId, DEFAULT_WAREHOUSE_ID);
        if (inventory == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "库存记录不存在");
        }
        int before = inventory.getAvailableQty() != null ? inventory.getAvailableQty() : 0;
        if (before < qty) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "库存不足");
        }
        int updated = inventoryMapper.reserveStock(skuId, qty);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "库存不足");
        }
        String reservationNo = "RES" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 20).toUpperCase();
        reservationMapper.insert(ShopInventoryReservation.builder()
                .reservationNo(reservationNo)
                .orderId(orderId)
                .orderItemId(orderItemId)
                .skuId(skuId)
                .qty(qty)
                .status("RESERVED")
                .expireTime(expireTime)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        inventoryLogMapper.insert(ShopInventoryLog.builder()
                .skuId(skuId)
                .bizType("ORDER_RESERVE")
                .bizId(orderId)
                .changeAvailable(-qty)
                .changeLocked(qty)
                .beforeAvailable(before)
                .afterAvailable(before - qty)
                .idempotencyKey("ORDER_RESERVE:" + orderItemId)
                .remark("订单预占库存")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void confirmOrderStock(Long orderId) {
        for (ShopInventoryReservation reservation : reservationMapper.selectReservedByOrderId(orderId)) {
            int updated = inventoryMapper.confirmReservedStock(reservation.getSkuId(), reservation.getQty());
            if (updated != 1) {
                throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "库存确认失败");
            }
            reservation.setStatus("CONFIRMED");
            reservation.setUpdatedAt(LocalDateTime.now());
            reservationMapper.updateById(reservation);
            ShopInventory inventory = inventoryMapper.selectBySkuIdAndWarehouse(
                    reservation.getSkuId(), DEFAULT_WAREHOUSE_ID);
            int available = inventory != null && inventory.getAvailableQty() != null
                    ? inventory.getAvailableQty() : 0;
            inventoryLogMapper.insert(ShopInventoryLog.builder()
                    .skuId(reservation.getSkuId())
                    .bizType("ORDER_CONFIRM")
                    .bizId(orderId)
                    .changeAvailable(0)
                    .changeLocked(-reservation.getQty())
                    .beforeAvailable(available)
                    .afterAvailable(available)
                    .idempotencyKey("ORDER_CONFIRM:" + reservation.getReservationNo())
                    .remark("订单支付确认库存")
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    @Transactional
    public void restoreRefundedStock(Long orderId, Long orderItemId, Long skuId, int qty,
                                     String remark) {
        if (skuId == null || qty <= 0) {
            return;
        }
        ShopInventory inventory = inventoryMapper.selectBySkuIdAndWarehouse(
                skuId, DEFAULT_WAREHOUSE_ID);
        int before = inventory != null && inventory.getAvailableQty() != null
                ? inventory.getAvailableQty() : 0;
        int updated = inventoryMapper.restoreSoldStock(skuId, qty);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "售后库存回补失败");
        }
        inventoryLogMapper.insert(ShopInventoryLog.builder()
                .skuId(skuId)
                .bizType("ORDER_REFUND")
                .bizId(orderId)
                .changeAvailable(qty)
                .changeLocked(0)
                .beforeAvailable(before)
                .afterAvailable(before + qty)
                .idempotencyKey("ORDER_REFUND:" + orderItemId + ":" + UUID.randomUUID())
                .remark(remark)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void releaseOrderStock(Long orderId) {
        for (ShopInventoryReservation reservation : reservationMapper.selectReservedByOrderId(orderId)) {
            int updated = inventoryMapper.releaseReservedStock(reservation.getSkuId(), reservation.getQty());
            if (updated != 1) {
                throw new BusinessException(ErrorCode.SERVER_ERROR.getCode(), "库存释放失败");
            }
            reservation.setStatus("RELEASED");
            reservation.setUpdatedAt(LocalDateTime.now());
            reservationMapper.updateById(reservation);
            ShopInventory inventory = inventoryMapper.selectBySkuIdAndWarehouse(
                    reservation.getSkuId(), DEFAULT_WAREHOUSE_ID);
            int available = inventory != null && inventory.getAvailableQty() != null
                    ? inventory.getAvailableQty() : 0;
            inventoryLogMapper.insert(ShopInventoryLog.builder()
                    .skuId(reservation.getSkuId())
                    .bizType("ORDER_RELEASE")
                    .bizId(orderId)
                    .changeAvailable(reservation.getQty())
                    .changeLocked(-reservation.getQty())
                    .beforeAvailable(available - reservation.getQty())
                    .afterAvailable(available)
                    .idempotencyKey("ORDER_RELEASE:" + reservation.getReservationNo())
                    .remark("订单取消释放库存")
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

}
