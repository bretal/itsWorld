import { requireLogin } from '../../../utils/auth';
import { getStoreDetail } from '../../../services/commerce';
import type { CommerceStoreDetail } from '../../../services/commerce';

const DAY_NAMES = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

interface HourItem {
  dayText: string;
  timeText: string;
  closed: boolean;
}

Page({
  data: {
    id: 0,
    loading: true,
    store: null as CommerceStoreDetail | null,
    addressText: '',
    hours: [] as HourItem[],
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const id = Number(options.id || 0);
    this.setData({ id });
    this.loadDetail(id);
  },

  async loadDetail(id: number) {
    try {
      const store = await getStoreDetail(id);
      const hours = (store.hours || []).map((hour) => ({
        dayText: DAY_NAMES[hour.dayOfWeek - 1] || `星期${hour.dayOfWeek}`,
        timeText: hour.closed ? '停业' : `${hour.openTime} - ${hour.closeTime}`,
        closed: hour.closed === 1,
      }));
      this.setData({
        store,
        addressText: [store.province, store.city, store.district, store.address]
          .filter(Boolean)
          .join(''),
        hours,
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  callStore() {
    const phone = this.data.store?.phone;
    if (phone) {
      wx.makePhoneCall({ phoneNumber: phone });
    }
  },
});
