package com.itsworld.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 充值单展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechargeOrderView {

    private Long id;

    private String rechargeNo;

    private Long userId;

    private String username;

    private Long planId;

    private String planName;

    private Long principalAmount;

    private Long bonusAmount;

    private String channel;

    private String paymentMethod;

    private String storeName;

    private Long operatorId;

    private Long approverId;

    private LocalDateTime approvedAt;

    private String rejectReason;

    private String externalTxNo;

    private String paymentStatus;

    private String creditStatus;

    private String planSnapshot;

    private String remark;

    private LocalDateTime paidAt;

    private LocalDateTime creditedAt;

    private LocalDateTime createdAt;

    /**
     * 开发环境是否允许模拟支付
     */
    private Boolean mockPayEnabled;

}
