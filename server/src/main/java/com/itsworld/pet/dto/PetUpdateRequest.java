package com.itsworld.pet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 宠物更新请求DTO
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetUpdateRequest {

    /**
     * 宠物名称
     */
    private String petName;

    /**
     * 宠物头像URL
     */
    private String avatarUrl;

    /**
     * 品种
     */
    private String breed;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private String birthDate;

    /**
     * 体重
     */
    private BigDecimal weight;

    /**
     * 身高（cm）
     */
    private BigDecimal height;

    /**
     * 宠物描述
     */
    private String description;

}
