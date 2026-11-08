package com.itsworld.commerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建支付请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateRequest {

    /**
     * WALLET / MOCK_WECHAT / WECHAT（WECHAT 未接入）
     */
    @NotBlank(message = "支付渠道不能为空")
    private String channel;

}
