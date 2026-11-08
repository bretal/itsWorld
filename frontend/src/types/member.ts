// 会员相关类型定义

import type { WalletSummary } from './wallet';

export interface MemberBenefit {
  id: number;
  benefitName: string;
  benefitType?: string;
  benefitValue?: string;
  description?: string;
}

export interface MemberLevel {
  id: number;
  levelCode: string;
  levelName: string;
  growthThreshold: number;
  iconUrl?: string;
  discountRate?: number;
  description?: string;
  status?: number;
  sortOrder?: number;
  current?: boolean;
  benefits?: MemberBenefit[];
}

export interface MemberOverview {
  userId: number;
  levelId?: number;
  levelCode?: string;
  levelName: string;
  growthValue: number;
  pointsBalance: number;
  nextLevelId?: number;
  nextLevelName?: string;
  nextLevelGrowth?: number;
  growthToNext?: number;
  progressPercent: number;
  benefits: MemberBenefit[];
  wallet: WalletSummary;
}

export interface MemberTask {
  id: number;
  taskCode: string;
  taskName: string;
  description?: string;
  periodType: string; // DAILY / WEEKLY / ONCE
  targetValue: number;
  pointsReward: number;
  growthReward: number;
  progress: number;
  rewardStatus: number; // 0 未领取, 1 已领取
  claimable: boolean;
}

export interface MemberAwardResult {
  pointsAwarded: number;
  growthAwarded: number;
  levelUp: boolean;
  levelName?: string;
}

export interface MemberGrowthLog {
  id: number;
  userId: number;
  changeValue: number;
  balanceAfter: number;
  sourceType: string;
  sourceNo: string;
  remark?: string;
  createdAt: string;
}

export interface MemberPointsLog {
  id: number;
  userId: number;
  changeValue: number;
  balanceAfter: number;
  sourceType: string;
  sourceNo: string;
  remark?: string;
  createdAt: string;
}

export interface MemberLevelHistory {
  id: number;
  userId: number;
  fromLevelId?: number;
  toLevelId?: number;
  growthValue: number;
  reason?: string;
  createdAt: string;
}

export interface Coupon {
  id: number;
  couponCode: string;
  templateName: string;
  couponType: string; // CASH / DISCOUNT
  amountCents?: number;
  discountRate?: number;
  minAmountCents?: number;
  description?: string;
  status: string; // AVAILABLE / LOCKED / USED / EXPIRED
  validFrom: string;
  validTo?: string;
  usedAt?: string;
  createdAt: string;
}

export interface MemberGift {
  id: number;
  giftName: string;
  description?: string;
  requiredLevelId?: number;
  requiredLevelName?: string;
  pointsReward: number;
  couponTemplateId?: number;
  couponTemplateName?: string;
  startAt?: string;
  endAt?: string;
  claimed: boolean;
  claimable: boolean;
  lockReason?: string;
}

export interface AdminMemberAccount {
  userId: number;
  username: string;
  nickName?: string;
  phone?: string;
  email?: string;
  role?: string;
  status?: number;
  lastLoginAt?: string;
  levelId?: number;
  levelName?: string;
  growthValue: number;
  pointsBalance: number;
  principalBalance: number;
  bonusBalance: number;
  availableBalance: number;
  createdAt: string;
}

export interface UserStatusRequest {
  status: number;
}

// ---------- 管理端请求类型 ----------

export interface MemberBenefitRequest {
  benefitName: string;
  benefitType?: string;
  benefitValue?: string;
  description?: string;
}

export interface MemberLevelRequest {
  levelCode: string;
  levelName: string;
  growthThreshold: number;
  iconUrl?: string;
  discountRate?: number;
  description?: string;
  status?: number;
  sortOrder?: number;
  benefits?: MemberBenefitRequest[];
}

export interface MemberTaskRequest {
  taskCode: string;
  taskName: string;
  description?: string;
  periodType: string;
  targetValue: number;
  pointsReward?: number;
  growthReward?: number;
  status?: number;
  sortOrder?: number;
}

export interface AdminMemberTask {
  id: number;
  taskCode: string;
  taskName: string;
  description?: string;
  periodType: string;
  targetValue: number;
  pointsReward: number;
  growthReward: number;
  status: number;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CouponTemplate {
  id: number;
  templateName: string;
  couponType: string;
  amountCents?: number;
  discountRate?: number;
  minAmountCents?: number;
  totalCount: number;
  issuedCount: number;
  validDays?: number;
  description?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CouponTemplateRequest {
  templateName: string;
  couponType: string;
  amountCents?: number;
  discountRate?: number;
  minAmountCents?: number;
  totalCount: number;
  validDays?: number;
  description?: string;
  status?: number;
}

export interface CouponIssueRequest {
  userId: number;
  templateId: number;
  quantity?: number;
}

export interface MemberGiftEntity {
  id: number;
  giftName: string;
  description?: string;
  requiredLevelId?: number;
  pointsReward: number;
  couponTemplateId?: number;
  startAt?: string;
  endAt?: string;
  status: number;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MemberGiftRequest {
  giftName: string;
  description?: string;
  requiredLevelId?: number;
  pointsReward?: number;
  couponTemplateId?: number;
  startAt?: string;
  endAt?: string;
  status?: number;
  sortOrder?: number;
}
