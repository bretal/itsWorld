package com.itsworld.commerce.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 员工班次保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffShiftSaveRequest {

    @NotNull(message = "班次日期不能为空")
    private LocalDate shiftDate;

    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    @NotNull(message = "班次容量不能为空")
    @Min(value = 1, message = "班次容量至少为1")
    private Integer capacity;

    private String status;

}
