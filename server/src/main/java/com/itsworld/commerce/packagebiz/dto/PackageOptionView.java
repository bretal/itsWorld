package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 套餐选项组展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageOptionView {

    private Long id;

    private String optionCode;

    private String optionName;

    private Integer minSelect;

    private Integer maxSelect;

    private Integer required;

    private List<PackageOptionItemView> items;

}
