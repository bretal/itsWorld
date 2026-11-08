package com.itsworld.commerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付结果DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResultView {

    private String orderNo;

    private String paymentOrderId;

    private String payChannel;

    private Long payAmount;

    private String status;

    private String externalTxNo;

    private LocalDateTime paidTime;

    private String orderStatus;

    private String paymentStatus;

}
