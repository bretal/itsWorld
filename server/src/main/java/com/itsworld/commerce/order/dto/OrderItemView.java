package com.itsworld.commerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单项展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemView {

    private Long id;

    private String itemType;

    private Long targetId;

    private Long skuId;

    private Integer quantity;

    private String titleSnapshot;

    private String specSnapshot;

    private Long unitPrice;

    private Long originalTotal;

    private Long discountAllocated;

    private Long payableTotal;

    private Integer fulfilledQty;

    private Long refundedAmount;

}
