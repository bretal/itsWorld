import { requireLogin } from '../../../utils/auth';
import { getAfterSale } from '../../../services/commerce';
import type { AfterSaleView } from '../../../services/commerce';
import { formatDateTime, formatMoney } from '../../../utils/format';

const STATUS_TEXT: Record<string, string> = {
  APPLIED: '待审核',
  REVIEWING: '审核中',
  APPROVED: '审核通过，待退款',
  REJECTED: '已拒绝',
  REFUNDING: '退款中',
  REFUNDED: '退款完成',
  REFUND_FAILED: '退款失败',
};

const TYPE_TEXT: Record<string, string> = {
  REFUND_ONLY: '仅退款',
  RETURN_REFUND: '退货退款',
};

Page({
  data: {
    afterSaleNo: '',
    loading: true,
    afterSale: null as AfterSaleView | null,
    items: [] as any[],
    statusText: '',
    typeText: '',
    amountText: '0.00',
    createdAtText: '',
    refundedAtText: '',
    logs: [] as any[],
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    this.setData({ afterSaleNo: options.afterSaleNo || '' });
  },

  onShow() {
    if (this.data.afterSaleNo) {
      this.load();
    }
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    try {
      const afterSale = await getAfterSale(this.data.afterSaleNo);
      this.setData({
        afterSale,
        items: (afterSale.items || []).map((item) => ({
          ...item,
          amountText: formatMoney(item.amount),
        })),
        statusText: STATUS_TEXT[afterSale.status] || afterSale.status,
        typeText: TYPE_TEXT[afterSale.type] || afterSale.type,
        amountText: formatMoney(afterSale.amount),
        createdAtText: formatDateTime(afterSale.createdAt),
        refundedAtText: afterSale.refundedAt
          ? formatDateTime(afterSale.refundedAt)
          : '',
        logs: (afterSale.logs || []).map((log) => ({
          ...log,
          createdAtText: formatDateTime(log.createdAt),
        })),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  previewEvidence(event: any) {
    const current = event.currentTarget.dataset.url;
    wx.previewImage({
      current,
      urls: this.data.afterSale?.evidenceUrls || [],
    });
  },

  goOrder() {
    if (this.data.afterSale?.orderNo) {
      wx.navigateTo({
        url: `/pages/commerce/order/detail?orderNo=${this.data.afterSale.orderNo}`,
      });
    }
  },

  goList() {
    wx.redirectTo({ url: '/pages/commerce/after-sale/list' });
  },
});
