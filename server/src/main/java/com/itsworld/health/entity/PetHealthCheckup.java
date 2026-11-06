package com.itsworld.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物体检记录实体
 * 对应数据库表 pet_health_checkup
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("pet_health_checkup")
public class PetHealthCheckup implements Serializable {

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
     * 体检日期
     */
    private LocalDate checkupDate;

    /**
     * 体检医院
     */
    private String vetClinic;

    /**
     * 体检发现
     */
    private String findings;

    /**
     * 医生建议
     */
    private String recommendations;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
