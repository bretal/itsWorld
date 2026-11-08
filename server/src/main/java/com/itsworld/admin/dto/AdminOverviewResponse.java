package com.itsworld.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理后台工作台总览
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOverviewResponse {

    private Long totalUsers;

    private Long activeUsers;

    private Long disabledUsers;

    private Long adminUsers;

    private Long memberCount;

    private Long totalPrincipalBalance;

    private Long totalBonusBalance;

    private Long totalAvailableBalance;

    private Long todayRechargeAmount;

    private Long pendingOfflineRecharges;

}
