package com.itsworld.commerce.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品SPU保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopProductSaveRequest {

    @NotBlank(message = "商品编码不能为空")
    private String productCode;

    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subtitle;

    private String description;

    private String coverUrl;

    private String saleStatus;

    private Integer sort;

}
