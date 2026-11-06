package com.itsworld.health.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 疫苗记录创建请求
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccineRecordCreateRequest {

    /**
     * 宠物ID
     */
    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    /**
     * 疫苗名称
     */
    @NotBlank(message = "疫苗名称不能为空")
    @Size(max = 100, message = "疫苗名称长度不能超过100字符")
    private String vaccineName;

    /**
     * 接种日期
     */
    @NotNull(message = "接种日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate vaccinatedAt;

    /**
     * 下次接种日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueAt;

    /**
     * 接种医院
     */
    @Size(max = 100, message = "医院名称长度不能超过100字符")
    private String vetClinic;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500字符")
    private String notes;

}
