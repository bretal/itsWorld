package com.itsworld.member.entity;

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
 * 用户优惠券实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long templateId;

    private String couponCode;

    private String status;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private LocalDateTime usedAt;

    /**
     * 锁定订单号（Phase 2 新增）
     */
    private String lockOrderNo;

    /**
     * 核销订单号（Phase 2 新增）
     */
    private String usedOrderNo;

    /**
     * 版本号（Phase 2 新增）
     */
    private Integer version;

    private LocalDateTime createdAt;

}
