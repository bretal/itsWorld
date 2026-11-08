package com.itsworld.commerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结算商品项DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutItemView {

    private Long cartItemId;

    private Long skuId;

    private Long productId;

    private String productName;

    private String productCoverUrl;

    private String skuCode;

    private String specJson;

    private Integer quantity;

    private Long unitPrice;

    private Long memberUnitPrice;

    private Long originalTotal;

    private Long memberDiscount;

    private Long payableTotal;

    private Integer availableQty;

    private String invalidReason;

}
