package com.itsworld.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 钱包资金流水展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionView {

    private Long id;

    private String txNo;

    private Long userId;

    private String username;

    private String txType;

    private String businessType;

    private String businessNo;

    private Long principalDelta;

    private Long bonusDelta;

    private Long principalAfter;

    private Long bonusAfter;

    private String remark;

    private LocalDateTime createdAt;

}
