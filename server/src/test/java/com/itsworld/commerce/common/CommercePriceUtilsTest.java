package com.itsworld.commerce.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 商业价格计算工具单元测试
 *
 * @author Claude
 * @date 2026-11-07
 */
class CommercePriceUtilsTest {

    @Test
    void shouldReturnOriginalPriceWhenRateIsNull() {
        assertEquals(3990L, CommercePriceUtils.applyDiscount(3990L, null));
    }

    @Test
    void shouldApplyMemberDiscountWithHalfUpRounding() {
        assertEquals(3910L, CommercePriceUtils.applyDiscount(3990L, new BigDecimal("0.98")));
        assertEquals(11564L, CommercePriceUtils.applyDiscount(11800L, new BigDecimal("0.98")));
    }

    @Test
    void shouldApplyFullDiscount() {
        assertEquals(1000L, CommercePriceUtils.applyDiscount(1000L, BigDecimal.ONE));
        assertEquals(0L, CommercePriceUtils.applyDiscount(1000L, BigDecimal.ZERO));
    }

}
