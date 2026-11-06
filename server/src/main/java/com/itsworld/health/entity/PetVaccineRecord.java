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
 * 宠物疫苗接种记录实体
 * 对应数据库表 pet_vaccine_record
 *
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("pet_vaccine_record")
public class PetVaccineRecord implements Serializable {

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
     * 疫苗名称
     */
    private String vaccineName;

    /**
     * 接种日期
     */
    private LocalDate vaccinatedAt;

    /**
     * 下次接种日期
     */
    private LocalDate nextDueAt;

    /**
     * 接种医院
     */
    private String vetClinic;

    /**
     * 备注
     */
    private String notes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
