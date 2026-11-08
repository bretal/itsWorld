package com.itsworld.commerce.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 时段创建请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSlotSaveRequest {

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    private Long staffId;

    @NotNull(message = "日期不能为空")
    private LocalDate serviceDate;

    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    @NotNull(message = "容量不能为空")
    @Min(value = 1, message = "容量至少为1")
    private Integer totalCapacity;

    private String status;

}
