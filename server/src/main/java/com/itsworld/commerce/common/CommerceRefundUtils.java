package com.itsworld.commerce.common;

/**
 * 商业售后金额计算工具
 *
 * @author Claude
 * @date 2026-11-07
 */
public final class CommerceRefundUtils {

    private CommerceRefundUtils() {
    }

    /**
     * 计算订单项售后可退金额（分）
     * 全额售后取剩余可退金额；部分售后按购买数量比例向下取整，并受剩余可退金额上限约束。
     *
     * @param payableTotal   订单项实付金额
     * @param refundedAmount 已退金额
     * @param quantity       购买数量
     * @param qty            本次售后数量
     * @return 可退金额，非法参数返回0
     */
    public static long resolveRefundAmount(long payableTotal, long refundedAmount,
                                           int quantity, int qty) {
        if (quantity <= 0 || qty <= 0) {
            return 0L;
        }
        long remaining = Math.max(0L, payableTotal - refundedAmount);
        if (qty >= quantity) {
            return remaining;
        }
        return Math.min(remaining, payableTotal * qty / quantity);
    }

}
