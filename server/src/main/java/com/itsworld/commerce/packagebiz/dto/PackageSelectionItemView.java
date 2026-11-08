package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 套餐包含/选中的服务项
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageSelectionItemView {

    private Long serviceId;

    private String serviceName;

    private Integer times;

    /**
     * 该服务项产生的加价（分），固定项为模板项加价
     */
    private Long addPrice;

    /**
     * 是否来自选项选择
     */
    private Boolean fromOption;

}
