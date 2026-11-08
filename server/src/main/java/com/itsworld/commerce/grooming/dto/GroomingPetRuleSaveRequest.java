package com.itsworld.commerce.grooming.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 美容服务宠物适配规则保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroomingPetRuleSaveRequest {

    @NotBlank(message = "宠物类型不能为空")
    private String speciesCode;

    private Integer minWeightGram;

    private Integer maxWeightGram;

    private Integer minAgeMonth;

    private Integer maxAgeMonth;

    private String note;

}
