package com.itsworld.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 钱包余额摘要DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletSummaryResponse {

    private Long principalBalance;

    private Long bonusBalance;

    private Long principalFrozen;

    private Long bonusFrozen;

    /**
     * 可用余额 = 本金 + 赠送 - 冻结
     */
    private Long availableBalance;

    /**
     * 总余额 = 本金 + 赠送（含冻结）
     */
    private Long totalBalance;

    /**
     * 当前环境是否允许模拟支付
     */
    private Boolean mockPayEnabled;

}
