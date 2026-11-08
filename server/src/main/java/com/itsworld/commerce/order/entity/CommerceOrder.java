package com.itsworld.commerce.order.entity;

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
 * 商业业务订单实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("commerce_order")
public class CommerceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String orderType;

    private String orderStatus;

    private String paymentStatus;

    private String fulfillmentStatus;

    private Long originalAmount;

    private Long discountAmount;

    private Long payableAmount;

    private Long paidAmount;

    private Long refundedAmount;

    private Long couponId;

    private String addressSnapshot;

    private String storeSnapshot;

    private LocalDateTime expireTime;

    private LocalDateTime paidTime;

    private LocalDateTime closeTime;

    private String cancelReason;

    private String remark;

    private String idempotencyKey;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
