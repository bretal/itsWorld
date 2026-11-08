package com.itsworld.commerce.aftersale.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 售后审核请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfterSaleReviewRequest {

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String remark;

}
