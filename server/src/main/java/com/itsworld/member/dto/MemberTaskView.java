package com.itsworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员任务展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberTaskView {

    private Long id;

    private String taskCode;

    private String taskName;

    private String description;

    private String periodType;

    private Integer targetValue;


    private Integer growthReward;

    private Integer progress;

    private Integer rewardStatus;

    private Boolean claimable;

}
