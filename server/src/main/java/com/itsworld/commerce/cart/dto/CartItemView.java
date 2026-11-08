package com.itsworld.commerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 购物车项展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemView {

    private Long id;

    private Long skuId;

    private Long productId;

    private String productName;

    private String productCoverUrl;

    private String skuCode;

    private String specJson;

    private Long salePrice;

    private Long memberPrice;

    private Integer quantity;

    private Integer selected;

    private Integer availableQty;

    private String invalidReason;

}
