package com.itsworld.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 钱包业务支付结果DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPaymentResult {

    private String txNo;

    private Long paidAmount;

    private Long principalDelta;

    private Long bonusDelta;

}
