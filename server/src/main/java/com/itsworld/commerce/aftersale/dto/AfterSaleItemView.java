package com.itsworld.commerce.aftersale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 售后明细展示
 *
 * @author Claude
 * @date 2026-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfterSaleItemView {

    private Long id;

    private Long orderItemId;

    private Long skuId;

    private String titleSnapshot;

    private Integer qty;

    private Long amount;

}
