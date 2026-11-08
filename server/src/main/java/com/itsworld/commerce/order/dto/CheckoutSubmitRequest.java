package com.itsworld.commerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交订单请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutSubmitRequest {

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;

    private Long couponId;

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    private String remark;

}
