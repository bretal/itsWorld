package com.itsworld.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 宠物喂食记录实体
 * 对应数据库表 pet_feeding_record
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("pet_feeding_record")
public class PetFeedingRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 餐次 / 饲料类型（breakfast-早饭, dinner-晚饭）
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
