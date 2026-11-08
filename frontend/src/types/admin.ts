// 管理后台工作台总览类型

export interface AdminOverview {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  adminUsers: number;
  memberCount: number;
  totalPrincipalBalance: number;
  totalBonusBalance: number;
  totalAvailableBalance: number;
  todayRechargeAmount: number;
  pendingOfflineRecharges: number;
}
