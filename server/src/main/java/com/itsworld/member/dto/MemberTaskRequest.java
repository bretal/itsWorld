package com.itsworld.member.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员任务配置请求DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberTaskRequest {

    @NotBlank(message = "任务编码不能为空")
    private String taskCode;

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    private String description;

    @NotBlank(message = "任务周期不能为空")
    private String periodType;

    @NotNull(message = "目标次数不能为空")
    @Min(value = 1, message = "目标次数至少为1")
    private Integer targetValue;


    @Min(value = 0, message = "成长值奖励不能为负数")
    private Integer growthReward;

    private Integer status;

    private Integer sortOrder;

}
