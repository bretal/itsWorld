package com.itsworld.commerce.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品分类保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopCategorySaveRequest {

    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String iconUrl;

    private Integer sort;

    private String status;

}
