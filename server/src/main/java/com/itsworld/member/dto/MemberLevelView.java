package com.itsworld.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员等级展示DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLevelView {

    private Long id;

    private String levelCode;

    private String levelName;

    private Integer growthThreshold;

    private String iconUrl;

    private BigDecimal discountRate;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private Boolean current;

    private List<MemberBenefitView> benefits;

}
