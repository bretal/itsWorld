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
 * 钱包余额调账审计实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("wallet_adjustment")
public class WalletAdjustment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String adjustmentNo;

    private Long userId;

    private Long principalDelta;

    private Long bonusDelta;

    private Long principalBefore;

    private Long principalAfter;

    private Long bonusBefore;

    private Long bonusAfter;

    private String reason;

    private Long operatorId;

    private String status;

    private LocalDateTime createdAt;

}
