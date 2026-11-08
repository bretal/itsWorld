import { get, post } from './api';
import type { PageData } from '@/types/common';
import type {
  RechargeCreateRequest,
  RechargeOrder,
  RechargePlan,
  WalletSummary,
  WalletTransaction,
} from '@/types/wallet';

/** 钱包余额摘要 */
export const getWallet = (): Promise<WalletSummary> => {
  return get('/api/wallet');
};

/** 启用中的充值方案 */
export const getRechargePlans = (): Promise<RechargePlan[]> => {
  return get('/api/wallet/recharge-plans');
};

/** 我的充值单 */
export const getRecharges = (page = 1, size = 10): Promise<PageData<RechargeOrder>> => {
  return get('/api/wallet/recharges', { params: { page, size } });
};

/** 创建线上充值单 */
export const createRecharge = (data: RechargeCreateRequest): Promise<RechargeOrder> => {
  return post('/api/wallet/recharges', data);
};

/** 查询充值单 */
export const getRecharge = (rechargeNo: string): Promise<RechargeOrder> => {
  return get(`/api/wallet/recharges/${rechargeNo}`);
};

/** 开发环境模拟支付 */
export const mockPay = (rechargeNo: string): Promise<RechargeOrder> => {
  return post(`/api/wallet/recharges/${rechargeNo}/mock-pay`);
};

/** 资金流水 */
export const getTransactions = (page = 1, size = 10): Promise<PageData<WalletTransaction>> => {
  return get('/api/wallet/transactions', { params: { page, size } });
};

export default {
  getWallet,
  getRechargePlans,
  getRecharges,
  createRecharge,
  getRecharge,
  mockPay,
  getTransactions,
};
