package com.itsworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会员礼包展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftView {

    private Long id;

    private String giftName;

    private String description;

    private Long requiredLevelId;

    private String requiredLevelName;

    private Integer growthReward;

    private Long couponTemplateId;

    private String couponTemplateName;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Boolean claimed;

    private Boolean claimable;

    private String lockReason;

}
