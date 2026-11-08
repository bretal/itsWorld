package com.itsworld.commerce.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存调整请求（正数增加，负数减少）
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustRequest {

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "调整数量不能为空")
    private Integer changeQty;

    @NotBlank(message = "调整原因不能为空")
    private String reason;

}
