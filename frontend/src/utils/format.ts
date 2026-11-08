/**
 * 分 -> 元（保留两位小数）
 */
export const formatMoney = (cents?: number | null): string => {
  return ((cents || 0) / 100).toFixed(2);
};

/**
 * 元 -> 分（四舍五入到整数分）
 */
export const yuanToCents = (yuan?: number | null): number => {
  return Math.round((yuan || 0) * 100);
};

/**
 * 分 -> 元数字（用于 InputNumber）
 */
export const centsToYuan = (cents?: number | null): number => {
  return (cents || 0) / 100;
};

/**
 * 折扣率 -> 展示文案，如 0.9 -> 9折、0.95 -> 9.5折
 */
export const formatDiscount = (rate?: number | null): string => {
  if (rate === undefined || rate === null) {
    return '-';
  }
  const discount = rate * 10;
  return `${Number.isInteger(discount) ? discount : discount.toFixed(1)}折`;
};

/**
 * 时间展示：2026-11-07T20:16:03 -> 2026-11-07 20:16
 */
export const formatDateTime = (value?: string | null): string => {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
};

/**
 * 金额变动展示（正数带 +）
 */
export const formatMoneyDelta = (cents?: number | null): string => {
  const value = cents || 0;
  const prefix = value > 0 ? '+' : '';
  return `${prefix}${formatMoney(value)}`;
};
