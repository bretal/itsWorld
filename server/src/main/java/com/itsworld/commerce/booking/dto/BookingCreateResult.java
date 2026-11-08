package com.itsworld.commerce.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建预约结果
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateResult {

    private Long bookingId;

    private String bookingNo;

    private String status;

    /**
     * 在线支付时生成的订单ID；套餐次数预约时为空
     */
    private Long orderId;

    /**
     * 在线支付时生成的订单号；套餐次数预约时为空
     */
    private String orderNo;

    /**
     * 在线支付应付金额（分）；套餐次数预约为0
     */
    private Long payableAmount;

    /**
     * 在线支付超时时间
     */
    private LocalDateTime expireTime;

}
