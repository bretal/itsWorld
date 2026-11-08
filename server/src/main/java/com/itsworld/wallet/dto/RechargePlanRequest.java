package com.itsworld.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 充值方案配置请求DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechargePlanRequest {

    @NotBlank(message = "方案名称不能为空")
    private String planName;

    @NotNull(message = "本金金额不能为空")
    @Min(value = 1, message = "本金金额必须大于0")
    private Long principalAmount;

    @Min(value = 0, message = "赠送金额不能为负数")
    private Long bonusAmount;

    @Min(value = 0, message = "赠送有效天数不能为负数")
    private Integer bonusValidDays;

    private String description;

    private Integer status;

    private Integer sortOrder;

}
