package com.itsworld.commerce.packagebiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 套餐固定服务项请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageItemRequest {

    @NotNull(message = "服务ID不能为空")
    private Long serviceId;

    @NotNull(message = "包含次数不能为空")
    @Min(value = 1, message = "包含次数至少为1")
    private Integer includedTimes;

    private Long extraPrice;

    private String selectableGroup;

    private Integer sort;

}
