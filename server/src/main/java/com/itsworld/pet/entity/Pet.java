package com.itsworld.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 宠物档案实体
 * 对应数据库表 pet_profile
 *
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("pet_profile")
public class Pet implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 宠物ID（自增）
     */
    @TableId(type = IdType.AUTO)
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
     * 宠物头像URL（MinIO存储路径）
     */
    private String avatarUrl;

    /**
     * 宠物类型（dog-狗, cat-猫, rabbit-兔子等）
     */
    private String petType;

    /**
     * 品种
     */
    private String breed;

    /**
     * 性别 (0-未知 1-公 2-母)
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private String birthDate;

    /**
     * 当前体重（kg）
     */
    @TableField("weight_kg")
    private BigDecimal weight;

    /**
     * 宠物描述
     */
    @TableField("bio")
    private String description;

    /**
     * 是否为当前活跃宠物 (1-是 0-否)
     */
    @TableField("is_active")
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    private LocalDateTime deletedAt;

}
