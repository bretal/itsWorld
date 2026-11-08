import { requireLogin } from '../../../utils/auth';
import { listBookings } from '../../../services/commerce';
import type { BookingView } from '../../../services/commerce';

const STATUS_TEXT: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  CONFIRMED: '已确认',
  CHECKED_IN: '已签到',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
};

const FILTERS = [
  { key: '', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待支付' },
  { key: 'CONFIRMED', label: '已确认' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
];

interface BookingCard extends BookingView {
  statusText: string;
  dateText: string;
  timeText: string;
}

Page({
  data: {
    filters: FILTERS,
    activeStatus: '',
    loading: true,
    loadingMore: false,
    page: 1,
    size: 10,
    hasMore: true,
    list: [] as BookingCard[],
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.load(true);
  },

  onPullDownRefresh() {
    this.load(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.load(false);
    }
  },

  onFilterChange(event: any) {
    const status = event.currentTarget.dataset.key;
    this.setData({ activeStatus: status });
    this.load(true);
  },

  async load(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true } : { loadingMore: true });
    try {
      const result = await listBookings(
        this.data.activeStatus || undefined,
        page,
        this.data.size,
      );
      const list = (result.records || []).map((booking) => ({
        ...booking,
        statusText: STATUS_TEXT[booking.status] || booking.status,
        dateText: booking.serviceDate || '',
        timeText:
          booking.startTime && booking.endTime
            ? `${String(booking.startTime).slice(0, 5)}-${String(booking.endTime).slice(0, 5)}`
            : '',
      }));
      this.setData({
        list: reset ? list : this.data.list.concat(list),
        page: result.pageNum,
        hasMore: result.pageNum < result.totalPages,
        loading: false,
        loadingMore: false,
      });
    } catch (error) {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  goDetail(event: any) {
    const bookingNo = event.currentTarget.dataset.no;
    wx.navigateTo({
      url: `/pages/commerce/booking/detail?bookingNo=${bookingNo}`,
    });
  },

  goGrooming() {
    wx.navigateTo({ url: '/pages/commerce/grooming/index' });
  },
});
