import { get } from './api';
import type { AdminOverview } from '@/types/admin';

/** 管理后台工作台总览 */
export const getOverview = (): Promise<AdminOverview> => {
  return get('/api/admin/overview');
};

export default { getOverview };
