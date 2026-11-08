package com.itsworld.commerce.fulfillment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发货明细展示
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentItemView {

    private Long orderItemId;

    private Long skuId;

    private String titleSnapshot;

    private Integer qty;

}
