package com.itsworld.commerce.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店员工保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSaveRequest {

    @NotBlank(message = "员工姓名不能为空")
    private String staffName;

    private String mobile;

    private String roleCode;

    private String status;

}
