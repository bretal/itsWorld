package com.itsworld.member.dto;

import jakarta.validation.Valid;
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
import java.util.List;

/**
 * 会员等级配置请求DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLevelRequest {

    @NotBlank(message = "等级编码不能为空")
    private String levelCode;

    @NotBlank(message = "等级名称不能为空")
    private String levelName;

    @NotNull(message = "成长值门槛不能为空")
    @Min(value = 0, message = "成长值门槛不能为负数")
    private Integer growthThreshold;

    private String iconUrl;

    @DecimalMin(value = "0", message = "折扣率不能小于0")
    @DecimalMax(value = "1", message = "折扣率不能大于1")
    private BigDecimal discountRate;

    private String description;

    private Integer status;

    private Integer sortOrder;

    @Valid
    private List<MemberBenefitRequest> benefits;

}
