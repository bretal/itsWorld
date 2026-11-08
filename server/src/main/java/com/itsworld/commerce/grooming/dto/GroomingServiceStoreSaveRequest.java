package com.itsworld.commerce.grooming.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店美容服务价格保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroomingServiceStoreSaveRequest {

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    @NotNull(message = "门店售价不能为空")
    @Min(value = 0, message = "门店售价不能为负数")
    private Long salePrice;

    @NotNull(message = "每时段容量不能为空")
    @Min(value = 1, message = "每时段容量至少为1")
    private Integer capacityPerSlot;

    private String status;

}
