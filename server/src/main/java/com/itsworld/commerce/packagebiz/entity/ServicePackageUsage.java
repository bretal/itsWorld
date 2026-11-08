package com.itsworld.commerce.packagebiz.entity;

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
 * 套餐使用核销流水实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("service_package_usage")
public class ServicePackageUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String usageNo;

    private Long userPackageId;

    private Long packageItemId;

    private Long bookingId;

    private Long serviceId;

    private Integer qty;

    private String status;

    private Long operatorId;

    private LocalDateTime usedTime;

    private String idempotencyKey;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
