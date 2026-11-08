package com.itsworld.commerce.aftersale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 售后申请明细
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfterSaleApplyItemRequest {

    @NotNull(message = "订单项ID不能为空")
    private Long orderItemId;

    @NotNull(message = "申请数量不能为空")
    @Min(value = 1, message = "申请数量至少为1")
    private Integer qty;

}
