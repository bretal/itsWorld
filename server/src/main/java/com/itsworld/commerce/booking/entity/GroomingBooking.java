package com.itsworld.commerce.booking.entity;

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
 * 美容预约实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("grooming_booking")
public class GroomingBooking implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bookingNo;

    private Long userId;

    private Long petId;

    private Long storeId;

    private Long staffId;

    private Long slotId;

    private Long orderId;

    private Long serviceId;

    private String status;

    private LocalDateTime reserveExpireTime;

    private LocalDateTime checkinTime;

    private LocalDateTime completedTime;

    private String cancelReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
