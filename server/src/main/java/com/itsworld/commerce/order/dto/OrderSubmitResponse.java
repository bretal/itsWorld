package com.itsworld.commerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 提交订单响应
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSubmitResponse {

    private Long orderId;

    private String orderNo;

    private Long payableAmount;

    private LocalDateTime expireTime;

}
