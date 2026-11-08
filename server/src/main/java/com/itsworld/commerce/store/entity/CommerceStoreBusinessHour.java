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
 * 门店营业时间实体
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("commerce_store_business_hour")
public class CommerceStoreBusinessHour implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;

    private Integer dayOfWeek;

    private LocalTime openTime;

    private LocalTime closeTime;

    private LocalDate specialDate;

    private Integer closed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
