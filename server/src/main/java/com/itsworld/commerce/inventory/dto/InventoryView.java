package com.itsworld.commerce.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryView {

    private Long id;

    private Long skuId;

    private Long productId;

    private String productName;

    private String productCoverUrl;

    private String skuCode;

    private String specJson;

    private Long salePrice;

    private Integer availableQty;

    private Integer lockedQty;

    private Integer soldQty;

    private String skuStatus;

}
