package com.itsworld.commerce.packagebiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 套餐购买试算请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagePurchasePreviewRequest {

    @NotNull(message = "套餐ID不能为空")
    private Long templateId;

    /**
     * 选中的选项候选服务ID
     */
    private List<Long> optionItemIds;

    /**
     * 可选优惠券
     */
    private Long couponId;

}
