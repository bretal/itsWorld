package com.itsworld.commerce.fulfillment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发货单展示
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentView {

    private Long id;

    private String shipmentNo;

    private Long orderId;

    private String orderNo;

    private String deliveryCompany;

    private String trackingNo;

    private String status;

    private LocalDateTime shippedTime;

    private LocalDateTime receivedTime;

    private String remark;

    private List<ShipmentItemView> items;

}
