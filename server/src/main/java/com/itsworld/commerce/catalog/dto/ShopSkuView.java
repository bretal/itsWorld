package com.itsworld.commerce.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopSkuView {

    private Long id;

    private Long productId;

    private String skuCode;

    private String specJson;

    private Long salePrice;

    private Long marketPrice;

    private Long memberPrice;

    private Integer weightGram;

    private String status;

    private Integer availableQty;

}
