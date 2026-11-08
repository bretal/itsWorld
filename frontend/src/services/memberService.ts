import { get, post } from './api';
import type { PageData } from '@/types/common';
import type {
  Coupon,
  MemberAwardResult,
  MemberGift,
  MemberGrowthLog,
  MemberLevel,
  MemberLevelHistory,
  MemberOverview,
  MemberPointsLog,
  MemberTask,
} from '@/types/member';

/** 会员中心总览 */
export const getOverview = (): Promise<MemberOverview> => {
  return get('/api/member/overview');
};

/** 等级与权益列表 */
export const getLevels = (): Promise<MemberLevel[]> => {
  return get('/api/member/levels');
};

/** 成长值流水 */
export const getGrowthLogs = (page = 1, size = 10): Promise<PageData<MemberGrowthLog>> => {
  return get('/api/member/growth-logs', { params: { page, size } });
};

/** 积分流水 */
export const getPointsLogs = (page = 1, size = 10): Promise<PageData<MemberPointsLog>> => {
  return get('/api/member/points-logs', { params: { page, size } });
};

/** 等级变更历史 */
export const getLevelHistory = (): Promise<MemberLevelHistory[]> => {
  return get('/api/member/level-history');
};

/** 当前任务列表 */
export const getTasks = (): Promise<MemberTask[]> => {
  return get('/api/member/tasks');
};

/** 每日签到 */
export const checkIn = (): Promise<void> => {
  return post('/api/member/tasks/check-in');
};

/** 领取任务奖励 */
export const claimTask = (taskId: number): Promise<MemberAwardResult> => {
  return post(`/api/member/tasks/${taskId}/claim`);
};

/** 我的优惠券 */
export const getCoupons = (status?: string): Promise<Coupon[]> => {
  return get('/api/member/coupons', { params: { status } });
};

/** 礼包列表 */
export const getGifts = (): Promise<MemberGift[]> => {
  return get('/api/member/gifts');
};

/** 领取礼包 */
export const claimGift = (giftId: number): Promise<void> => {
  return post(`/api/member/gifts/${giftId}/claim`);
};

export default {
  getOverview,
  getLevels,
  getGrowthLogs,
  getPointsLogs,
  getLevelHistory,
  getTasks,
  checkIn,
  claimTask,
  getCoupons,
  getGifts,
  claimGift,
};
