package com.itsworld.commerce.aftersale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 售后申请请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfterSaleApplyRequest {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * REFUND_ONLY-仅退款；RETURN_REFUND-退货退款
     */
    @NotBlank(message = "售后类型不能为空")
    private String type;

    @NotBlank(message = "申请原因不能为空")
    private String reason;

    private String description;

    private List<String> evidenceUrls;

    @Valid
    @NotEmpty(message = "请选择售后商品")
    private List<AfterSaleApplyItemRequest> items;

}
