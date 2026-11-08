package com.itsworld.commerce.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 可预约时段展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSlotView {

    private Long id;

    private Long storeId;

    private Long staffId;

    private LocalDate serviceDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer totalCapacity;

    private Integer reservedCapacity;

    private Integer lockedCapacity;

    private Integer availableCapacity;

    private String status;

}
