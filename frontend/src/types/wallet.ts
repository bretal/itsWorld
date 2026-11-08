// 钱包相关类型定义（金额单位：分）

export interface WalletSummary {
  principalBalance: number;
  bonusBalance: number;
  principalFrozen: number;
  bonusFrozen: number;
  availableBalance: number;
  totalBalance: number;
  mockPayEnabled?: boolean;
}

export interface WalletTransaction {
  id: number;
  txNo: string;
  userId: number;
  username?: string;
  txType: string;
  businessType?: string;
  businessNo?: string;
  principalDelta: number;
  bonusDelta: number;
  principalAfter: number;
  bonusAfter: number;
  remark?: string;
  createdAt: string;
}

export interface RechargePlan {
  id: number;
  planName: string;
  principalAmount: number;
  bonusAmount: number;
  bonusValidDays: number;
  description?: string;
  status: number;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface RechargeOrder {
  id: number;
  rechargeNo: string;
  userId: number;
  username?: string;
  planId?: number;
  planName?: string;
  principalAmount: number;
  bonusAmount: number;
  channel: string; // ONLINE / OFFLINE
  paymentMethod?: string;
  storeName?: string;
  operatorId?: number;
  approverId?: number;
  approvedAt?: string;
  rejectReason?: string;
  externalTxNo?: string;
  paymentStatus: string; // CREATED / PAID / CLOSED / EXCEPTION
  creditStatus: string; // PENDING / CREDITED
  planSnapshot?: string;
  remark?: string;
  paidAt?: string;
  creditedAt?: string;
  createdAt: string;
  mockPayEnabled?: boolean;
}

export interface RechargePlanRequest {
  planName: string;
  principalAmount: number;
  bonusAmount?: number;
  bonusValidDays?: number;
  description?: string;
  status?: number;
  sortOrder?: number;
}

export interface RechargeCreateRequest {
  planId: number;
}

export interface OfflineRechargeCreateRequest {
  userId: number;
  planId: number;
  storeName: string;
  remark?: string;
}

export interface OfflineRechargeActionRequest {
  externalTxNo?: string;
  rejectReason?: string;
}

export interface WalletAdjustment {
  id: number;
  adjustmentNo: string;
  userId: number;
  username?: string;
  principalDelta: number;
  bonusDelta: number;
  principalBefore: number;
  principalAfter: number;
  bonusBefore: number;
  bonusAfter: number;
  reason: string;
  operatorId: number;
  operatorName?: string;
  status: string;
  createdAt: string;
}

export interface AdjustBalanceRequest {
  userId: number;
  principalDelta: number;
  bonusDelta: number;
  reason: string;
}
