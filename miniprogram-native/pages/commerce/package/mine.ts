import { requireLogin } from '../../../utils/auth';
import { listMyPackages, listPackageUsages } from '../../../services/commerce';
import type { PackageUsageView, UserPackageView } from '../../../services/commerce';
import { formatDateTime } from '../../../utils/format';

const STATUS_TEXT: Record<string, string> = {
  ACTIVE: '生效中',
  USED_UP: '已用完',
  EXPIRED: '已过期',
  CANCELLED: '已取消',
};

const FILTERS = [
  { key: '', label: '全部' },
  { key: 'ACTIVE', label: '生效中' },
  { key: 'USED_UP', label: '已用完' },
  { key: 'EXPIRED', label: '已过期' },
];

interface PackageCard extends UserPackageView {
  statusText: string;
  expireText: string;
  itemTexts: { name: string; remaining: number; total: number }[];
}

Page({
  data: {
    filters: FILTERS,
    activeStatus: '',
    loading: true,
    list: [] as PackageCard[],
    expandedId: 0,
    usages: [] as PackageUsageView[],
    usageLoading: false,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.load();
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  onFilterChange(event: any) {
    this.setData({ activeStatus: event.currentTarget.dataset.key, expandedId: 0 });
    this.load();
  },

  async load() {
    try {
      this.setData({ loading: true });
      const result = await listMyPackages(
        this.data.activeStatus || undefined,
        1,
        50,
      );
      this.setData({
        list: (result.records || []).map((item) => ({
          ...item,
          statusText: STATUS_TEXT[item.status] || item.status,
          expireText: item.expireTime
            ? `${formatDateTime(item.expireTime)} 到期`
            : '长期有效',
          itemTexts: (item.items || []).map((pkgItem) => ({
            serviceId: pkgItem.serviceId,
            name: pkgItem.serviceName || '服务',
            remaining: pkgItem.remainingTimes,
            total: pkgItem.totalTimes,
          })),
        })),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async toggleUsages(event: any) {
    const userPackageId = Number(event.currentTarget.dataset.id);
    if (this.data.expandedId === userPackageId) {
      this.setData({ expandedId: 0, usages: [] });
      return;
    }
    try {
      this.setData({ expandedId: userPackageId, usageLoading: true, usages: [] });
      const usages = await listPackageUsages(userPackageId);
      this.setData({ usages, usageLoading: false });
    } catch (error) {
      this.setData({ usageLoading: false });
    }
  },

  goPackages() {
    wx.navigateTo({ url: '/pages/commerce/package/index' });
  },

  goBooking(event: any) {
    const serviceId = Number(event.currentTarget.dataset.serviceId);
    const storeId = Number(event.currentTarget.dataset.storeId || 0);
    const petId = Number(event.currentTarget.dataset.petId || 0);
    wx.navigateTo({
      url: `/pages/commerce/booking/index?serviceId=${serviceId}&storeId=${storeId}&petId=${petId}`,
    });
  },
});
