import { requireLogin } from '../../../utils/auth';
import { listStores } from '../../../services/commerce';
import type { CommerceStore } from '../../../services/commerce';

interface StoreItem extends CommerceStore {
  addressText: string;
}

Page({
  data: {
    stores: [] as StoreItem[],
    loading: true,
  },

  onLoad() {
    if (!requireLogin()) {
      return;
    }
    this.loadStores();
  },

  onPullDownRefresh() {
    this.loadStores().finally(() => wx.stopPullDownRefresh());
  },

  async loadStores() {
    try {
      const result = await listStores(1, 50);
      const stores = (result.records || []).map((store) => ({
        ...store,
        addressText: [store.province, store.city, store.district, store.address]
          .filter(Boolean)
          .join(''),
      }));
      this.setData({ stores, loading: false });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  goDetail(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    wx.navigateTo({ url: `/pages/commerce/store/detail?id=${id}` });
  },
});
