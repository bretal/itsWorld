package com.itsworld.member.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会员礼包配置请求DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberGiftRequest {

    @NotBlank(message = "礼包名称不能为空")
    private String giftName;

    private String description;

    private Long requiredLevelId;

    @Min(value = 0, message = "成长值奖励不能为负数")
    private Integer growthReward;

    private Long couponTemplateId;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Integer status;

    private Integer sortOrder;

}
