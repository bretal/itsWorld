package com.itsworld.commerce.inventory.service;

import com.itsworld.commerce.inventory.dto.InventoryAdjustRequest;
import com.itsworld.commerce.inventory.dto.InventoryLogView;
import com.itsworld.commerce.inventory.dto.InventoryView;
import com.itsworld.common.result.PageResult;

import java.time.LocalDateTime;

/**
 * 库存业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface InventoryService {

    PageResult<InventoryView> pageInventory(long pageNum, long pageSize, String keyword);

    InventoryView adjustStock(Long operatorId, InventoryAdjustRequest request);

    PageResult<InventoryLogView> pageLogs(Long skuId, long pageNum, long pageSize);

    /**
     * 订单预占库存
     */
    void reserveStock(Long orderId, Long orderItemId, Long skuId, int qty, LocalDateTime expireTime);

    /**
     * 支付确认库存（锁定转已售）
     */
    void confirmOrderStock(Long orderId);

    /**
     * 取消/关单释放库存
     */
    void releaseOrderStock(Long orderId);

    /**
     * 售后退货回补库存：sold → available
     */
    void restoreRefundedStock(Long orderId, Long orderItemId, Long skuId, int qty, String remark);

}
