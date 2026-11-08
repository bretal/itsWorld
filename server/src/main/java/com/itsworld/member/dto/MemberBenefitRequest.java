package com.itsworld.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员权益配置请求DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberBenefitRequest {

    @NotBlank(message = "权益名称不能为空")
    private String benefitName;

    private String benefitType;

    private String benefitValue;

    private String description;

}
