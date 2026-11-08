import { requireLogin } from '../../../utils/auth';
import { listOrders } from '../../../services/commerce';
import type { OrderDetail } from '../../../services/commerce';
import { formatDateTime, formatMoney } from '../../../utils/format';

const STATUS_TEXT: Record<string, string> = {
  WAIT_PAY: '待支付',
  PAID: '已支付',
  PROCESSING: '处理中',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  CLOSED: '已关闭',
};

interface OrderView extends OrderDetail {
  statusText: string;
  payableText: string;
  timeText: string;
  itemCount: number;
  firstTitle: string;
}

Page({
  data: {
    tabs: [
      { key: '', label: '全部' },
      { key: 'WAIT_PAY', label: '待支付' },
      { key: 'PAID', label: '已支付' },
      { key: 'CANCELLED', label: '已取消' },
      { key: 'CLOSED', label: '已关闭' },
    ],
    activeTab: '',
    orders: [] as OrderView[],
    loading: true,
    loadingMore: false,
    page: 1,
    size: 10,
    hasMore: true,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.loadOrders(true);
  },

  onPullDownRefresh() {
    this.loadOrders(true).finally(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      this.loadOrders(false);
    }
  },

  async loadOrders(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1;
    this.setData(reset ? { loading: true } : { loadingMore: true });
    try {
      const result = await listOrders(
        this.data.activeTab || undefined,
        page,
        this.data.size,
      );
      const orders = (result.records || []).map((order) => ({
        ...order,
        statusText: STATUS_TEXT[order.orderStatus] || order.orderStatus,
        payableText: formatMoney(order.payableAmount),
        timeText: formatDateTime(order.createdAt),
        itemCount: order.items?.reduce((sum, item) => sum + item.quantity, 0) || 0,
        firstTitle: order.items?.[0]?.titleSnapshot || '订单商品',
      }));
      this.setData({
        orders: reset ? orders : this.data.orders.concat(orders),
        page: result.pageNum,
        hasMore: result.pageNum < result.totalPages,
        loading: false,
        loadingMore: false,
      });
    } catch (error) {
      this.setData({ loading: false, loadingMore: false });
    }
  },

  onTapTab(event: any) {
    const key = event.currentTarget.dataset.key || '';
    this.setData({ activeTab: key });
    this.loadOrders(true);
  },

  goDetail(event: any) {
    const orderNo = event.currentTarget.dataset.no;
    wx.navigateTo({
      url: `/pages/commerce/order/detail?orderNo=${orderNo}`,
    });
  },
});
