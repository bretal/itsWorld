package com.itsworld.commerce.store.entity;

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
 * 员工班次实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("commerce_staff_shift")
public class CommerceStaffShift implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long staffId;

    private Long storeId;

    private LocalDate shiftDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer capacity;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
