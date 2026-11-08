package com.itsworld.commerce.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 门店保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSaveRequest {

    @NotBlank(message = "门店编码不能为空")
    private String storeCode;

    @NotBlank(message = "门店名称不能为空")
    private String name;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String businessStatus;

    private String description;

}
