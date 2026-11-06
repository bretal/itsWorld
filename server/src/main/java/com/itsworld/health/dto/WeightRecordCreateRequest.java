package com.itsworld.health.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体重记录创建请求
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightRecordCreateRequest {

    /**
     * 宠物ID
     */
    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    /**
     * 体重（kg）
     */
    @NotNull(message = "体重不能为空")
    @DecimalMin(value = "0.01", message = "体重必须大于0")
    @DecimalMax(value = "999.99", message = "体重不能超过999.99kg")
    @Digits(integer = 3, fraction = 2, message = "体重最多3位整数、2位小数")
    private BigDecimal weightKg;

    /**
     * 记录时间；为空时使用当前时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordedAt;

}
