package com.itsworld.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 充值单实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("wallet_recharge_order")
public class WalletRechargeOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String rechargeNo;

    private Long userId;

    private Long planId;

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

    private LocalDateTime updatedAt;

}
