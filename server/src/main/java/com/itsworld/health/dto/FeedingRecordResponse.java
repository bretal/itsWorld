package com.itsworld.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 喂食记录响应
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedingRecordResponse {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 餐次 / 饲料类型
     */
    private String feedType;

    /**
     * 喂食量（克）
     */
    private BigDecimal amountG;

    /**
     * 喂食时间
     */
    private LocalDateTime mealTime;

    /**
     * 备注
     */
    private String notes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
