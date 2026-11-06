package com.itsworld.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体重记录响应
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightRecordResponse {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 体重（kg）
     */
    private BigDecimal weightKg;

    /**
     * 记录时间
     */
    private LocalDateTime recordedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
