package com.itsworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponView {

    private Long id;

    private String couponCode;

    private String templateName;

    private String couponType;

    private Long amountCents;

    private BigDecimal discountRate;

    private Long minAmountCents;

    private String description;

    private String status;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private LocalDateTime usedAt;

    private LocalDateTime createdAt;

}
