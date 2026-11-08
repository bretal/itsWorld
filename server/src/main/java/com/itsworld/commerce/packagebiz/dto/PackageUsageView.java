package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 套餐核销流水展示
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageUsageView {

    private Long id;

    private String usageNo;

    private Long userPackageId;

    private Long packageItemId;

    private Long bookingId;

    private String bookingNo;

    private Long serviceId;

    private String serviceName;

    private Integer qty;

    private String status;

    private LocalDateTime usedTime;

    private LocalDateTime createdAt;

}
