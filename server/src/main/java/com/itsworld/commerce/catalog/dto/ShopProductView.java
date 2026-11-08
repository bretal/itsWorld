package com.itsworld.commerce.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品列表展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopProductView {

    private Long id;

    private String productCode;

    private Long categoryId;

    private String categoryName;

    private String name;

    private String subtitle;

    private String coverUrl;

    private String saleStatus;

    private Long minPrice;

    private Long minMemberPrice;

    private Integer sort;

}
