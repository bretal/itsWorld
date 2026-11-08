package com.itsworld.commerce.packagebiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 套餐模板保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageTemplateSaveRequest {

    @NotBlank(message = "套餐编码不能为空")
    private String packageCode;

    @NotBlank(message = "套餐名称不能为空")
    private String name;

    private String coverUrl;

    private String description;

    @NotNull(message = "售价不能为空")
    @Min(value = 0, message = "售价不能为负数")
    private Long salePrice;

    private Integer validDays;

    private String status;

    private Integer allowCrossStore;

    private List<PackageItemRequest> items;

    private List<PackageOptionRequest> options;

}
