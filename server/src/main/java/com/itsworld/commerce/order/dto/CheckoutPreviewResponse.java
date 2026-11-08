package com.itsworld.commerce.order.dto;

import com.itsworld.member.dto.CouponOptionView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 结算预览响应
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPreviewResponse {

    private List<CheckoutItemView> items;

    private Long originalAmount;

    private Long memberDiscount;

    private Long couponDiscount;

    private Long shippingFee;

    private Long payableAmount;

    private List<CouponOptionView> couponOptions;

    private Long walletAvailableBalance;

    private Boolean valid;

}
