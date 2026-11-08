package com.itsworld.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 优惠券模板配置请求DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponTemplateRequest {

    @NotBlank(message = "券名称不能为空")
    private String templateName;

    @NotBlank(message = "券类型不能为空")
    private String couponType;

    @Min(value = 0, message = "券面额不能为负数")
    private Long amountCents;

    @DecimalMin(value = "0", message = "折扣率不能小于0")
    @DecimalMax(value = "1", message = "折扣率不能大于1")
    private BigDecimal discountRate;

    @Min(value = 0, message = "最低消费不能为负数")
    private Long minAmountCents;

    @NotNull(message = "发行总量不能为空")
    @Min(value = 1, message = "发行总量至少为1")
    private Integer totalCount;

    @Min(value = 1, message = "有效天数至少为1")
    private Integer validDays;

    private String description;

    private Integer status;

}
