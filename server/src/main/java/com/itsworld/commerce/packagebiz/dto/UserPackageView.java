package com.itsworld.commerce.packagebiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户已购套餐展示
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPackageView {

    private Long id;

    private String packageInstanceNo;

    private Long packageId;

    private String packageName;

    private Long petId;

    private String petName;

    private Long storeId;

    private String storeName;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime expireTime;

    private String selectionSnapshot;

    private List<UserPackageItemView> items;

}
