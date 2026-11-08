package com.itsworld.commerce.grooming.dto;

import com.itsworld.commerce.grooming.entity.GroomingServicePetRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 美容服务详情DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroomingServiceDetailView {

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

    private List<GroomingServicePetRule> petRules;

    private Boolean petEligible;

    private String ineligibleReason;

}
