package com.itsworld.health.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 喂食记录创建请求
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedingRecordCreateRequest {

    /**
     * 宠物ID
     */
    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    /**
     * 餐次（breakfast-早饭, dinner-晚饭）
     */
    @NotBlank(message = "餐次不能为空")
    @Size(max = 50, message = "餐次长度不能超过50字符")
    private String feedType;

    /**
     * 喂食量（克）
     */
    @DecimalMin(value = "0", message = "喂食量不能为负数")
    @Digits(integer = 6, fraction = 2, message = "喂食量最多6位整数、2位小数")
    private BigDecimal amountG;

    /**
     * 喂食时间；为空时使用当前时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mealTime;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500字符")
    private String notes;

}
