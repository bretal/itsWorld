package com.itsworld.commerce.packagebiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 套餐选项组请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageOptionRequest {

    @NotBlank(message = "选项编码不能为空")
    private String optionCode;

    @NotBlank(message = "选项名称不能为空")
    private String optionName;

    @NotNull(message = "最少选择不能为空")
    private Integer minSelect;

    @NotNull(message = "最多选择不能为空")
    private Integer maxSelect;

    private Integer required;

    private Integer sort;

    private List<PackageOptionItemRequest> items;

}
