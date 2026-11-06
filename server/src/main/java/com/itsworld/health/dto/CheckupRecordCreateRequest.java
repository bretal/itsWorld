package com.itsworld.health.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 体检记录创建请求
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckupRecordCreateRequest {

    /**
     * 宠物ID
     */
    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    /**
     * 体检日期
     */
    @NotNull(message = "体检日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkupDate;

    /**
     * 体检医院
     */
    @Size(max = 100, message = "医院名称长度不能超过100字符")
    private String vetClinic;

    /**
     * 体检发现
     */
    private String findings;

    /**
     * 医生建议
     */
    private String recommendations;

}
