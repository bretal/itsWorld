package com.itsworld.commerce.aftersale.dto;

import com.itsworld.commerce.aftersale.entity.CommerceAfterSaleLog;
import com.itsworld.commerce.aftersale.entity.CommerceRefundLink;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后单展示
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfterSaleView {

    private Long id;

    private String afterSaleNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String username;

    private String type;

    private String reason;

    private String description;

    private List<String> evidenceUrls;

    private Long amount;

    private String status;

    private String reviewRemark;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;

    private List<AfterSaleItemView> items;

    private List<CommerceAfterSaleLog> logs;

    private CommerceRefundLink refund;

}
