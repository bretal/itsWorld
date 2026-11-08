package com.itsworld.commerce.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品图片保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopProductImageSaveRequest {

    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    private Integer sort;

}
