package com.itsworld.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 钱包余额调账展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletAdjustmentView {

    private Long id;

    private String adjustmentNo;

    private Long userId;

    private String username;

    private Long principalDelta;

    private Long bonusDelta;

    private Long principalBefore;

    private Long principalAfter;

    private Long bonusBefore;

    private Long bonusAfter;

    private String reason;

    private Long operatorId;

    private String operatorName;

    private String status;

    private LocalDateTime createdAt;

}
