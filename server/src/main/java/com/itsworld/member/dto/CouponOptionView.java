package com.itsworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 结算可用优惠券选项DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponOptionView {

    private Long id;

    private String templateName;

    private String couponType;

    private Long amountCents;

    private BigDecimal discountRate;

    private Long minAmountCents;

    private Long discountAmount;

    private String description;

}
