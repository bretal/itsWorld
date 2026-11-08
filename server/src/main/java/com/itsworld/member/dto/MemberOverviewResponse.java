package com.itsworld.member.dto;

import com.itsworld.wallet.dto.WalletSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会员中心总览DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberOverviewResponse {

    private String membershipStatus;

    private java.time.LocalDateTime memberExpiresAt;

    private Long userId;

    private Long levelId;

    private String levelCode;

    private String levelName;

    private Integer growthValue;







    private List<MemberBenefitView> benefits;

    private WalletSummaryResponse wallet;

}
