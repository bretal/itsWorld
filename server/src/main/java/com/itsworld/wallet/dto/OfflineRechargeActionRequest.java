package com.itsworld.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 线下充值审批/驳回请求DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineRechargeActionRequest {

    /**
     * 线下收款凭证号（审批时可选）
     */
    private String externalTxNo;

    /**
     * 驳回原因（驳回时可选）
     */
    private String rejectReason;

}
