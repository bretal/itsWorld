package com.itsworld.commerce.order.event;

import com.itsworld.commerce.order.entity.CommerceOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商业订单状态变更事件
 * 用于订单模块与预约/套餐模块解耦：订单支付、取消、关闭后由各业务监听处理履约资源。
 *
 * @author Claude
 * @date 2026-11-07
 */
@Getter
@AllArgsConstructor
public class CommerceOrderStatusChangedEvent {

    /**
     * 状态动作：PAID / CANCELLED / CLOSED
     */
    public static final String ACTION_PAID = "PAID";

    public static final String ACTION_CANCELLED = "CANCELLED";

    public static final String ACTION_CLOSED = "CLOSED";

    /**
     * 订单快照
     */
    private final CommerceOrder order;

    /**
     * 状态动作
     */
    private final String action;

    /**
     * 操作方：USER / ADMIN / SYSTEM
     */
    private final String operatorType;

    /**
     * 操作人ID
     */
    private final Long operatorId;

}
