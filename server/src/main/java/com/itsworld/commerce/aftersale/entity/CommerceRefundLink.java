package com.itsworld.commerce.aftersale.entity;

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
 * 商业退款记录实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("commerce_refund_link")
public class CommerceRefundLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long afterSaleId;

    private Long paymentLinkId;

    private String channel;

    private Long amount;

    private String status;

    private String externalRefundNo;

    private String failReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
