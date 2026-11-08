package com.itsworld.commerce.cart.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改购物车项请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemUpdateRequest {

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    private Integer selected;

}
