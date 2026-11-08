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
 * 用户已购套餐实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user_service_package")
public class UserServicePackage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String packageInstanceNo;

    private Long userId;

    private Long petId;

    private Long packageId;

    private Long orderItemId;

    private Long storeId;

    private String nameSnapshot;

    private String selectionSnapshot;

    private LocalDateTime startTime;

    private LocalDateTime expireTime;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
