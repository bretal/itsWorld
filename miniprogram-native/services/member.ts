import { request } from '../utils/request';
import type { PageData } from '../types/page';

export interface Benefit { id: number; benefitName: string; benefitType: string; benefitValue?: string; description?: string }
export interface Overview {
  membershipStatus: 'NOT_OPEN' | 'ACTIVE' | 'EXPIRED'; levelId?: number; levelName?: string;
  memberExpiresAt?: string; growthValue: number; benefits: Benefit[];
  wallet: { availableBalance: number; totalBalance: number; mockPayEnabled: boolean };
}
export interface Task { id: number; taskCode: string; taskName: string; growthReward: number; progress: number; targetValue: number; rewardStatus: number; claimable: boolean }
export interface Gift { id: number; giftName: string; description?: string; growthReward: number; couponTemplateName?: string; claimable: boolean; claimed: boolean; lockReason?: string }
// MemberCenter JDBC endpoints use database-style field names.
export interface Plan { id: number; name: string; level_id: number; price: number; duration_days: number }
export interface Reward { id: number; name: string; growth_cost: number; stock: number }
export interface Redemption { id: number; redemption_no: string; name_snapshot: string; growth_cost: number; status: string; created_at: string }
export interface GrowthLog { id: number; changeValue: number; balanceAfter: number; remark: string; createdAt: string }
export interface WalletLog { id: number; principalDelta: number; bonusDelta: number; remark?: string; txType: string; createdAt: string }
export interface RechargePlan { id: number; planName: string; principalAmount: number; bonusAmount: number }

export const getMemberOverview = () => request<Overview>({ url: '/member/overview' });
export const getMemberTasks = () => request<Task[]>({ url: '/member/tasks' });
export const getMemberGifts = () => request<Gift[]>({ url: '/member/gifts' });
export const claimTask = (id: number) => request({ url: `/member/tasks/${id}/claim`, method: 'POST' });
export const claimGift = (id: number) => request({ url: `/member/gifts/${id}/claim`, method: 'POST' });
export const getPlans = () => request<Plan[]>({ url: '/member/subscription-plans' });
export const buyMembership = (planId: number, requestKey: string) => request<string>({ url: '/member/subscription-orders', method: 'POST', data: { planId, requestKey } });
export const getRewards = () => request<Reward[]>({ url: '/member/reward-products' });
export const redeemReward = (productId: number, requestKey: string) => request<string>({ url: '/member/redemptions', method: 'POST', data: { productId, requestKey } });
export const getRedemptions = (page: number) => request<Redemption[]>({ url: '/member/redemptions', data: { page, size: 20 } });
export const getGrowthLogs = (page: number) => request<PageData<GrowthLog>>({ url: '/member/growth-logs', data: { page, size: 20 } });
export const getWalletLogs = (page: number) => request<PageData<WalletLog>>({ url: '/wallet/transactions', data: { page, size: 20 } });
export const getRechargePlans = () => request<RechargePlan[]>({ url: '/wallet/recharge-plans' });
export const createRecharge = (planId: number) => request<{ rechargeNo: string }>({ url: '/wallet/recharges', method: 'POST', data: { planId } });
export const mockRecharge = (no: string) => request({ url: `/wallet/recharges/${no}/mock-pay`, method: 'POST' });
