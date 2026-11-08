package com.itsworld.commerce.store.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 营业时间保存请求
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessHourSaveRequest {

    @NotNull(message = "星期不能为空")
    @Min(value = 1, message = "星期取值为1-7")
    @Max(value = 7, message = "星期取值为1-7")
    private Integer dayOfWeek;

    @NotNull(message = "开始时间不能为空")
    private LocalTime openTime;

    @NotNull(message = "结束时间不能为空")
    private LocalTime closeTime;

    private LocalDate specialDate;

    private Integer closed;

}
