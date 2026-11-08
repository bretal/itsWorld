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
 * 用户套餐服务次数实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user_service_package_item")
public class UserServicePackageItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userPackageId;

    private Long serviceId;

    private Integer totalTimes;

    private Integer usedTimes;

    private Integer frozenTimes;

    private String serviceSnapshot;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
