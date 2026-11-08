package com.itsworld.commerce.booking.dto;

import com.itsworld.commerce.booking.entity.GroomingBookingLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 美容预约展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingView {

    private Long id;

    private String bookingNo;

    private Long userId;

    private String username;

    private Long petId;

    private String petName;

    private Long storeId;

    private String storeName;

    private Long staffId;

    private Long slotId;

    private LocalDate serviceDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Long orderId;

    private String orderNo;

    private Long serviceId;

    private String serviceName;

    private String status;

    private LocalDateTime reserveExpireTime;

    private LocalDateTime checkinTime;

    private LocalDateTime completedTime;

    private String cancelReason;

    private LocalDateTime createdAt;

    private List<GroomingBookingLog> logs;

}
