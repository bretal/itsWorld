package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 套餐购买结果
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePurchaseResult {

    private Long orderId;

    private String orderNo;

    private Long payableAmount;

    private LocalDateTime expireTime;

}
