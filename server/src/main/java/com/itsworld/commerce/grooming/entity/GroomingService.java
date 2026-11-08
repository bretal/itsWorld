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
 * 美容服务项目实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("grooming_service")
public class GroomingService implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String serviceCode;

    private String name;

    private String coverUrl;

    private String description;

    private Integer durationMin;

    private Long basePrice;

    private String status;

    private Integer sort;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
