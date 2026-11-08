import { get, post, put, del } from './api';
import type { PageData } from '@/types/common';
import type {
  AdjustBalanceRequest,
  OfflineRechargeActionRequest,
  OfflineRechargeCreateRequest,
  RechargeOrder,
  RechargePlan,
  RechargePlanRequest,
  WalletAdjustment,
  WalletTransaction,
} from '@/types/wallet';

/** 充值方案列表（含停用） */
export const getPlans = (): Promise<RechargePlan[]> => {
  return get('/api/admin/wallet/recharge-plans');
};

/** 新建充值方案 */
export const createPlan = (data: RechargePlanRequest): Promise<RechargePlan> => {
  return post('/api/admin/wallet/recharge-plans', data);
};

/** 更新充值方案 */
export const updatePlan = (planId: number, data: RechargePlanRequest): Promise<RechargePlan> => {
  return put(`/api/admin/wallet/recharge-plans/${planId}`, data);
};

/** 下架充值方案 */
export const disablePlan = (planId: number): Promise<void> => {
  return del(`/api/admin/wallet/recharge-plans/${planId}`);
};

/** 线下充值列表 */
export const getOfflineRecharges = (
  page = 1,
  size = 10,
  status?: string
): Promise<PageData<RechargeOrder>> => {
  return get('/api/admin/wallet/offline-recharges', { params: { page, size, status } });
};

/** 创建线下充值申请 */
export const createOfflineRecharge = (
  data: OfflineRechargeCreateRequest
): Promise<RechargeOrder> => {
  return post('/api/admin/wallet/offline-recharges', data);
};

/** 审批线下充值 */
export const approveOfflineRecharge = (
  rechargeNo: string,
  data?: OfflineRechargeActionRequest
): Promise<RechargeOrder> => {
  return post(`/api/admin/wallet/offline-recharges/${rechargeNo}/approve`, data || {});
};

/** 驳回线下充值 */
export const rejectOfflineRecharge = (
  rechargeNo: string,
  data?: OfflineRechargeActionRequest
): Promise<RechargeOrder> => {
  return post(`/api/admin/wallet/offline-recharges/${rechargeNo}/reject`, data || {});
};

/** 全部资金流水 */
export const getTransactions = (
  page = 1,
  size = 10,
  txType?: string
): Promise<PageData<WalletTransaction>> => {
  return get('/api/admin/wallet/transactions', { params: { page, size, txType } });
};

/** 余额调增/调减 */
export const adjustBalance = (data: AdjustBalanceRequest): Promise<WalletAdjustment> => {
  return post('/api/admin/wallet/adjustments', data);
};

/** 余额调账记录 */
export const getAdjustments = (
  page = 1,
  size = 10,
  userId?: number
): Promise<PageData<WalletAdjustment>> => {
  return get('/api/admin/wallet/adjustments', { params: { page, size, userId } });
};

export default {
  getPlans,
  createPlan,
  updatePlan,
  disablePlan,
  getOfflineRecharges,
  createOfflineRecharge,
  approveOfflineRecharge,
  rejectOfflineRecharge,
  getTransactions,
  adjustBalance,
  getAdjustments,
};
