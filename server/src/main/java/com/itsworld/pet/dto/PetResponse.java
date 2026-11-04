package com.itsworld.pet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 宠物信息响应DTO
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetResponse {

    /**
     * 宠物ID
     */
    private Long id;

    /**
     * 所有者用户ID
     */
    private Long userId;

    /**
     * 宠物名称
     */
    private String petName;

    /**
     * 宠物头像URL
     */
    private String avatarUrl;

    /**
     * 宠物类型
     */
    private String petType;

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
     * 当前体重
     */
    private BigDecimal weight;

    /**
     * 宠物描述
     */
    private String description;

}
