package com.itsworld.commerce.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 可预约时段实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("booking_slot")
public class BookingSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;

    private Long staffId;

    private LocalDate serviceDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer totalCapacity;

    private Integer reservedCapacity;

    private Integer lockedCapacity;

    private String status;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
