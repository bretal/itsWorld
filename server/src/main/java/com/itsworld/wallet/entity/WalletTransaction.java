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
 * 钱包资金交易主流水实体（追加式，不更新、不删除）
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("wallet_transaction")
public class WalletTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String txNo;

    private Long walletId;

    private Long userId;

    private String txType;

    private String businessType;

    private String businessNo;

    private String idempotencyKey;

    private Long principalDelta;

    private Long bonusDelta;

    private Long principalBefore;

    private Long principalAfter;

    private Long bonusBefore;

    private Long bonusAfter;

    private String remark;

    private LocalDateTime createdAt;

}
