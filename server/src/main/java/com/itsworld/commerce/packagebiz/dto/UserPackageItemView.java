package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户套餐服务项展示
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPackageItemView {

    private Long id;

    private Long serviceId;

    private String serviceName;

    private Integer totalTimes;

    private Integer usedTimes;

    private Integer frozenTimes;

    private Integer remainingTimes;

    /**
     * 服务快照 JSON
     */
    private String serviceSnapshot;

}
