package com.itsworld.commerce.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 售后金额计算工具单元测试
 *
 * @author Claude
 * @date 2026-11-07
 */
class CommerceRefundUtilsTest {

    @Test
    void shouldRefundRemainingWhenFullQuantity() {
        assertEquals(3000L, CommerceRefundUtils.resolveRefundAmount(3000L, 0L, 2, 2));
        assertEquals(1000L, CommerceRefundUtils.resolveRefundAmount(3000L, 2000L, 2, 2));
    }

    @Test
    void shouldRefundProportionallyForPartialQuantity() {
        assertEquals(1000L, CommerceRefundUtils.resolveRefundAmount(3000L, 0L, 3, 1));
        assertEquals(666L, CommerceRefundUtils.resolveRefundAmount(2000L, 0L, 3, 1));
    }

    @Test
    void shouldCapByRemainingAmount() {
        assertEquals(500L, CommerceRefundUtils.resolveRefundAmount(3000L, 2500L, 3, 1));
    }

    @Test
    void shouldReturnZeroForInvalidArguments() {
        assertEquals(0L, CommerceRefundUtils.resolveRefundAmount(3000L, 0L, 0, 1));
        assertEquals(0L, CommerceRefundUtils.resolveRefundAmount(3000L, 0L, 2, 0));
        assertEquals(0L, CommerceRefundUtils.resolveRefundAmount(3000L, 3000L, 2, 1));
    }

}
