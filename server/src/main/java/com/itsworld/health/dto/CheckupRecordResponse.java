package com.itsworld.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检记录响应
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckupRecordResponse {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 体检日期
     */
    private LocalDate checkupDate;

    /**
     * 体检医院
     */
    private String vetClinic;

    /**
     * 体检发现
     */
    private String findings;

    /**
     * 医生建议
     */
    private String recommendations;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
