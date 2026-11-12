package com.itsworld.commerce.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品SKU保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopSkuSaveRequest {

    @NotBlank(message = "SKU编码不能为空")
    private String skuCode;

    private String specJson;

    /** 仅新增 SKU 时生效，留空按零库存草稿处理。 */
    @Min(value = 0, message = "初始库存不能为负数")
    private Integer initialStock;

    @NotNull(message = "销售价不能为空")
    @Min(value = 0, message = "销售价不能为负数")
    private Long salePrice;

    @NotNull(message = "市场价不能为空")
    @Min(value = 0, message = "市场价不能为负数")
    private Long marketPrice;

    private Integer weightGram;

    private String status;

}
