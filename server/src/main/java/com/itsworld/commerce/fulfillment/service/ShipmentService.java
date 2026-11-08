package com.itsworld.commerce.fulfillment.service;

import com.itsworld.commerce.fulfillment.dto.ShipmentCreateRequest;
import com.itsworld.commerce.fulfillment.dto.ShipmentView;
import com.itsworld.common.result.PageResult;

/**
 * 物流发货业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface ShipmentService {

    /**
     * 管理端发货：仅商品订单，PAID/PROCESSING 状态
     */
    ShipmentView shipOrder(String orderNo, ShipmentCreateRequest request, Long operatorId);

    /**
     * 用户查看订单物流
     */
    ShipmentView getUserShipment(Long userId, String orderNo);

    /**
     * 管理端查看订单物流
     */
    ShipmentView getAdminShipment(String orderNo);

    PageResult<ShipmentView> pageAdminShipments(String status, String keyword,
                                                long pageNum, long pageSize);

    /**
     * 用户确认收货
     */
    void confirmReceipt(Long userId, String orderNo);

    /**
     * 发货 7 天后自动确认收货（定时任务调用）
     */
    void autoConfirmReceipts();

}
