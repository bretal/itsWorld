package com.itsworld.commerce.address.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收货地址保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressSaveRequest {

    @NotBlank(message = "收货人不能为空")
    private String recipientName;

    @NotBlank(message = "手机号不能为空")
    private String mobile;

    private String province;

    private String city;

    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String addressDetail;

    private Integer isDefault;

}
