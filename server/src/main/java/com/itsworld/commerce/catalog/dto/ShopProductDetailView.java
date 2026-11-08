package com.itsworld.commerce.catalog.dto;

import com.itsworld.commerce.catalog.entity.ShopProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品详情DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopProductDetailView {

    private Long id;

    private String productCode;

    private Long categoryId;

    private String categoryName;

    private String name;

    private String subtitle;

    private String description;

    private String coverUrl;

    private String saleStatus;

    private Integer sort;

    private List<ShopProductImage> images;

    private List<ShopSkuView> skus;

}
