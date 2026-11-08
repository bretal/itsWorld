import { requireLogin } from '../../../utils/auth';
import { listAfterSales } from '../../../services/commerce';
import type { AfterSaleView } from '../../../services/commerce';
import { formatDateTime, formatMoney } from '../../../utils/format';

const STATUS_TEXT: Record<string, string> = {
  APPLIED: '待审核',
  REVIEWING: '审核中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  REFUND_FAILED: '退款失败',
};

const TYPE_TEXT: Record<string, string> = {
  REFUND_ONLY: '仅退款',
  RETURN_REFUND: '退货退款',
};

const FILTERS = [
  { key: '', label: '全部' },
  { key: 'APPLIED', label: '待审核' },
  { key: 'APPROVED', label: '待退款' },
  { key: 'REFUNDED', label: '已退款' },
  { key: 'REJECTED', label: '已拒绝' },
];

interface AfterSaleCard extends AfterSaleView {
  statusText: string;
  typeText: string;
  amountText: string;
  createdAtText: string;
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
    list: [] as AfterSaleCard[],
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
    this.setData({ activeStatus: event.currentTarget.dataset.key });
    this.load(true);
  },

  async load(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true } : { loadingMore: true });
    try {
      const result = await listAfterSales(
        this.data.activeStatus || undefined,
        page,
        this.data.size,
      );
      const list = (result.records || []).map((item) => ({
        ...item,
        statusText: STATUS_TEXT[item.status] || item.status,
        typeText: TYPE_TEXT[item.type] || item.type,
        amountText: formatMoney(item.amount),
        createdAtText: formatDateTime(item.createdAt),
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
    const afterSaleNo = event.currentTarget.dataset.no;
    wx.navigateTo({
      url: `/pages/commerce/after-sale/detail?afterSaleNo=${afterSaleNo}`,
    });
  },
});
