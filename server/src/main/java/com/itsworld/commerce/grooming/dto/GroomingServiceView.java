package com.itsworld.commerce.grooming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 美容服务展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroomingServiceView {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String serviceCode;

    private String name;

    private String coverUrl;

    private String description;

    private Integer durationMin;

    private Long basePrice;

    private Long storePrice;

    private Long memberPrice;

    private String status;

    private Integer sort;

}
