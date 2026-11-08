package com.itsworld.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端发券请求DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponIssueRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "券模板ID不能为空")
    private Long templateId;

    @Min(value = 1, message = "发券数量至少为1")
    @Max(value = 100, message = "单次发券不能超过100张")
    private Integer quantity;

}
