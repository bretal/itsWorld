package com.itsworld.commerce.packagebiz.dto;

import com.itsworld.member.dto.CouponOptionView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 套餐购买试算结果
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePurchasePreview {

    private Long templateId;

    private String packageCode;

    private String packageName;

    private Long originalAmount;

    private Long memberDiscount;

    private Long couponDiscount;

    private Long payableAmount;

    private Long walletAvailableBalance;

    private List<PackageSelectionItemView> items;

    private List<CouponOptionView> couponOptions;

}
