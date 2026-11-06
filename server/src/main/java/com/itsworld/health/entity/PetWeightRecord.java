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
 * 宠物体重历史实体
 * 对应数据库表 pet_weight_record
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("pet_weight_record")
public class PetWeightRecord implements Serializable {

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
     * 体重（kg）
     */
    private BigDecimal weightKg;

    /**
     * 记录日期
     */
    private LocalDateTime recordedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
