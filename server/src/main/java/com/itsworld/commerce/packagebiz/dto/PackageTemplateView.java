package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 套餐模板展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageTemplateView {

    private Long id;

    private String packageCode;

    private String name;

    private String coverUrl;

    private String description;

    private Long salePrice;

    private Integer validDays;

    private String status;

    private Integer allowCrossStore;

    private List<PackageItemView> items;

    private List<PackageOptionView> options;

}
