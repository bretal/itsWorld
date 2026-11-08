package com.itsworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 优惠券试算结果DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponQuote {

    private Long couponId;

    private String couponName;

    private String couponType;

    private Long discountAmount;

    private Long amountBefore;

    private Long amountAfter;

    private BigDecimal discountRate;

}
