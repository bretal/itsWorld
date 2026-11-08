package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 套餐选项候选展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageOptionItemView {

    private Long id;

    private Long serviceId;

    private String serviceName;

    private Long addPrice;

    private Integer includedTimes;

    private String status;

}
