package com.itsworld.commerce.order.service;

import com.itsworld.commerce.order.dto.OrderDetailView;
import com.itsworld.common.result.PageResult;

/**
 * 商业订单业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface CommerceOrderService {

    PageResult<OrderDetailView> pageUserOrders(Long userId, String status, long pageNum, long pageSize);

    OrderDetailView getUserOrder(Long userId, String orderNo);

    PageResult<OrderDetailView> pageAdminOrders(String status, String keyword, long pageNum, long pageSize);

    OrderDetailView getAdminOrder(String orderNo);

    void cancelUserOrder(Long userId, String orderNo, String reason);

    void closeAdminOrder(String orderNo, String reason);

    void markPaid(Long orderId, String operatorType, Long operatorId);

    /**
     * 关闭已超时未支付订单（定时任务调用）
     */
    void closeExpiredOrders();

}
