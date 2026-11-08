package com.itsworld.commerce.order.entity;

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
 * 商业支付记录实体（兼作支付单）
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("commerce_payment_link")
public class CommercePaymentLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String paymentOrderId;

    private String payChannel;

    private Long payAmount;

    private String status;

    private String idempotencyKey;

    private String externalTxNo;

    private LocalDateTime paidTime;

    private String failReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
