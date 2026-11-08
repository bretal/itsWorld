package com.itsworld.commerce.order.dto;

import com.itsworld.commerce.order.entity.CommerceOrderDiscount;
import com.itsworld.commerce.order.entity.CommercePaymentLink;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailView {

    private Long id;

    private String orderNo;

    private Long userId;

    private String orderType;

    private String orderStatus;

    private String paymentStatus;

    private String fulfillmentStatus;

    private Long originalAmount;

    private Long discountAmount;

    private Long payableAmount;

    private Long paidAmount;

    private Long refundedAmount;

    private Long couponId;

    private String addressSnapshot;

    private String storeSnapshot;

    private LocalDateTime expireTime;

    private LocalDateTime paidTime;

    private LocalDateTime closeTime;

    private String cancelReason;

    private String remark;

    private LocalDateTime createdAt;

    private List<OrderItemView> items;

    private List<CommerceOrderDiscount> discounts;

    private CommercePaymentLink latestPayment;

}
