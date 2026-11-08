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
 * 商业售后单实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("commerce_after_sale")
public class CommerceAfterSale implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String afterSaleNo;

    private Long orderId;

    private Long userId;

    private String type;

    private String reason;

    private String description;

    private String evidenceUrls;

    private Long amount;

    private String status;

    private String reviewRemark;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
