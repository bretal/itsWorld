package com.itsworld.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商业订单钱包退款结果
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletRefundResult {

    private String refundNo;

    private Long refundedAmount;

    private Long principalRestored;

    private Long bonusRestored;

    private String status;

}
