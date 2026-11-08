package com.itsworld.commerce.grooming.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 美容服务保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroomingServiceSaveRequest {

    private Long categoryId;

    @NotBlank(message = "服务编码不能为空")
    private String serviceCode;

    @NotBlank(message = "服务名称不能为空")
    private String name;

    private String coverUrl;

    private String description;

    @NotNull(message = "服务时长不能为空")
    @Min(value = 1, message = "服务时长至少为1分钟")
    private Integer durationMin;

    @NotNull(message = "基础价不能为空")
    @Min(value = 0, message = "基础价不能为负数")
    private Long basePrice;

    private String status;

    private Integer sort;

}
