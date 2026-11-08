package com.itsworld.commerce.grooming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 美容服务宠物适配规则实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("grooming_service_pet_rule")
public class GroomingServicePetRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serviceId;

    private String speciesCode;

    private Integer minWeightGram;

    private Integer maxWeightGram;

    private Integer minAgeMonth;

    private Integer maxAgeMonth;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
