package com.itsworld.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端余额调账请求DTO（金额单位：分，可为负）
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustBalanceRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 本金变动，正数调增，负数调减
     */
    private Long principalDelta;

    /**
     * 赠送变动，正数调增，负数调减
     */
    private Long bonusDelta;

    @NotBlank(message = "调账原因不能为空")
    @Size(max = 255, message = "调账原因不能超过255个字符")
    private String reason;

}
