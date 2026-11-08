package com.itsworld.commerce.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 商业服务价格计算工具
 *
 * @author Claude
 * @date 2026-11-07
 */
public final class CommercePriceUtils {

    private CommercePriceUtils() {
    }

    /**
     * 按会员折扣乘数计算价格（分）
     *
     * @param price 原价（分）
     * @param rate  折扣乘数，如 0.95
     * @return 会员价（分）
     */
    public static long applyDiscount(long price, BigDecimal rate) {
        if (rate == null) {
            return price;
        }
        return BigDecimal.valueOf(price)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

}
