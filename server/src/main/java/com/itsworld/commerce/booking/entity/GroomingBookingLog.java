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
 * 美容预约状态日志实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("grooming_booking_log")
public class GroomingBookingLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private String fromStatus;

    private String toStatus;

    private String action;

    private String operatorType;

    private Long operatorId;

    private String remark;

    private LocalDateTime createdAt;

}
