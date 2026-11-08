package com.itsworld.commerce.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发货请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentCreateRequest {

    @NotBlank(message = "物流公司不能为空")
    private String deliveryCompany;

    @NotBlank(message = "物流单号不能为空")
    private String trackingNo;

    private String remark;

}
