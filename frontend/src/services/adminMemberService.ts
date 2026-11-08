import { get, post, put, del } from './api';
import type { PageData } from '@/types/common';
import type {
  AdminMemberAccount,
  AdminMemberTask,
  Coupon,
  CouponIssueRequest,
  CouponTemplate,
  CouponTemplateRequest,
  MemberGiftEntity,
  MemberGiftRequest,
  MemberGrowthLog,
  MemberLevel,
  MemberLevelRequest,
  MemberPointsLog,
  MemberTaskRequest,
  UserStatusRequest,
} from '@/types/member';
import type { RechargeOrder, WalletTransaction } from '@/types/wallet';

/** 会员账户分页（全量用户视图） */
export const getAccounts = (
  page = 1,
  size = 10,
  keyword?: string,
  status?: number,
  role?: string
): Promise<PageData<AdminMemberAccount>> => {
  return get('/api/admin/member/accounts', { params: { page, size, keyword, status, role } });
};

/** 启用/禁用用户 */
export const updateUserStatus = (userId: number, data: UserStatusRequest): Promise<void> => {
  return put(`/api/admin/member/accounts/${userId}/status`, data);
};

/** 指定用户成长值流水 */
export const getUserGrowthLogs = (
  userId: number,
  page = 1,
  size = 10
): Promise<PageData<MemberGrowthLog>> => {
  return get(`/api/admin/member/accounts/${userId}/growth-logs`, { params: { page, size } });
};

/** 指定用户积分流水 */
export const getUserPointsLogs = (
  userId: number,
  page = 1,
  size = 10
): Promise<PageData<MemberPointsLog>> => {
  return get(`/api/admin/member/accounts/${userId}/points-logs`, { params: { page, size } });
};

/** 指定用户钱包流水 */
export const getUserTransactions = (
  userId: number,
  page = 1,
  size = 10
): Promise<PageData<WalletTransaction>> => {
  return get(`/api/admin/member/accounts/${userId}/wallet-transactions`, {
    params: { page, size },
  });
};

/** 指定用户充值单 */
export const getUserRecharges = (
  userId: number,
  page = 1,
  size = 10
): Promise<PageData<RechargeOrder>> => {
  return get(`/api/admin/member/accounts/${userId}/recharges`, { params: { page, size } });
};

/** 指定用户优惠券 */
export const getUserCoupons = (userId: number, status?: string): Promise<Coupon[]> => {
  return get(`/api/admin/member/accounts/${userId}/coupons`, { params: { status } });
};

/** 等级列表（含停用） */
export const getLevels = (): Promise<MemberLevel[]> => {
  return get('/api/admin/member/levels');
};

/** 新建等级 */
export const createLevel = (data: MemberLevelRequest): Promise<MemberLevel> => {
  return post('/api/admin/member/levels', data);
};

/** 更新等级 */
export const updateLevel = (levelId: number, data: MemberLevelRequest): Promise<MemberLevel> => {
  return put(`/api/admin/member/levels/${levelId}`, data);
};

/** 下架等级 */
export const disableLevel = (levelId: number): Promise<void> => {
  return del(`/api/admin/member/levels/${levelId}`);
};

/** 任务列表 */
export const getTasks = (): Promise<AdminMemberTask[]> => {
  return get('/api/admin/member/tasks');
};

/** 新建任务 */
export const createTask = (data: MemberTaskRequest): Promise<AdminMemberTask> => {
  return post('/api/admin/member/tasks', data);
};

/** 更新任务 */
export const updateTask = (taskId: number, data: MemberTaskRequest): Promise<AdminMemberTask> => {
  return put(`/api/admin/member/tasks/${taskId}`, data);
};

/** 下架任务 */
export const disableTask = (taskId: number): Promise<void> => {
  return del(`/api/admin/member/tasks/${taskId}`);
};

/** 优惠券模板列表 */
export const getCouponTemplates = (): Promise<CouponTemplate[]> => {
  return get('/api/admin/member/coupon-templates');
};

/** 新建优惠券模板 */
export const createCouponTemplate = (data: CouponTemplateRequest): Promise<CouponTemplate> => {
  return post('/api/admin/member/coupon-templates', data);
};

/** 更新优惠券模板 */
export const updateCouponTemplate = (
  templateId: number,
  data: CouponTemplateRequest
): Promise<CouponTemplate> => {
  return put(`/api/admin/member/coupon-templates/${templateId}`, data);
};

/** 下架优惠券模板 */
export const disableCouponTemplate = (templateId: number): Promise<void> => {
  return del(`/api/admin/member/coupon-templates/${templateId}`);
};

/** 发券 */
export const issueCoupons = (data: CouponIssueRequest): Promise<void> => {
  return post('/api/admin/member/coupons/issue', data);
};

/** 礼包列表 */
export const getGifts = (): Promise<MemberGiftEntity[]> => {
  return get('/api/admin/member/gifts');
};

/** 新建礼包 */
export const createGift = (data: MemberGiftRequest): Promise<MemberGiftEntity> => {
  return post('/api/admin/member/gifts', data);
};

/** 更新礼包 */
export const updateGift = (giftId: number, data: MemberGiftRequest): Promise<MemberGiftEntity> => {
  return put(`/api/admin/member/gifts/${giftId}`, data);
};

/** 下架礼包 */
export const disableGift = (giftId: number): Promise<void> => {
  return del(`/api/admin/member/gifts/${giftId}`);
};

export default {
  getAccounts,
  updateUserStatus,
  getUserGrowthLogs,
  getUserPointsLogs,
  getUserTransactions,
  getUserRecharges,
  getUserCoupons,
  getLevels,
  createLevel,
  updateLevel,
  disableLevel,
  getTasks,
  createTask,
  updateTask,
  disableTask,
  getCouponTemplates,
  createCouponTemplate,
  updateCouponTemplate,
  disableCouponTemplate,
  issueCoupons,
  getGifts,
  createGift,
  updateGift,
  disableGift,
};
