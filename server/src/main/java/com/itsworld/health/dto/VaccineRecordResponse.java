package com.itsworld.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 疫苗记录响应
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccineRecordResponse {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 疫苗名称
     */
    private String vaccineName;

    /**
     * 接种日期
     */
    private LocalDate vaccinatedAt;

    /**
     * 下次接种日期
     */
    private LocalDate nextDueAt;

    /**
     * 接种医院
     */
    private String vetClinic;

    /**
     * 备注
     */
    private String notes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
