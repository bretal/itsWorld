package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 套餐固定项展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageItemView {

    private Long id;

    private Long serviceId;

    private String serviceName;

    private Integer includedTimes;

    private Long extraPrice;

    private String selectableGroup;

}
