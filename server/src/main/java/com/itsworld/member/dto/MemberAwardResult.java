package com.itsworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员奖励发放结果DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberAwardResult {


    private Integer growthAwarded;

    private Boolean levelUp;

    private String levelName;

}
