package com.itsworld.commerce.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 可用于预约的套餐次数项
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageBookingOptionView {

    /**
     * 用户套餐服务项ID，预约时作为 userPackageItemId 传入
     */
    private Long userPackageItemId;

    private Long userPackageId;

    private String packageInstanceNo;

    private String packageName;

    private Long petId;

    private String petName;

    private Long storeId;

    private String storeName;

    private Long serviceId;

    private String serviceName;

    private Integer totalTimes;

    private Integer usedTimes;

    private Integer frozenTimes;

    private Integer remainingTimes;

    private LocalDateTime expireTime;

}
