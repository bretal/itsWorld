package com.itsworld.commerce.store.dto;

import com.itsworld.commerce.store.entity.CommerceStoreBusinessHour;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店详情DTO
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDetailView {

    private Long id;

    private String storeCode;

    private String name;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String businessStatus;

    private String description;

    private List<CommerceStoreBusinessHour> hours;

}
