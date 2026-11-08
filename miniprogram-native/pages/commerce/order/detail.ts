import { requireLogin } from '../../../utils/auth';
import {
  cancelOrder,
  confirmReceipt,
  getOrder,
  getOrderReview,
  getShipment,
  payOrder,
} from '../../../services/commerce';
import type { OrderDetail, OrderItem, ReviewView } from '../../../services/commerce';
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

const PAYMENT_TEXT: Record<string, string> = {
  UNPAID: '未支付',
  PAID: '已支付',
  PARTIAL_REFUND: '部分退款',
  REFUNDED: '已退款',
};

interface OrderItemView extends OrderItem {
  specText: string;
  priceText: string;
  lineTotalText: string;
}

Page({
  data: {
    orderNo: '',
    loading: true,
    paying: false,
    order: null as OrderDetail | null,
    statusText: '',
    paymentStatusText: '',
    items: [] as OrderItemView[],
    addressText: '',
    originalText: '0.00',
    discountText: '0.00',
    payableText: '0.00',
    paidText: '0.00',
    canPay: false,
    canCancel: false,
    hasShipment: false,
    canAfterSale: false,
    review: null as ReviewView | null,
    paidTimeText: '',
    paymentChannelText: '',
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const orderNo = options.orderNo || '';
    this.setData({ orderNo });
    this.loadOrder();
  },

  onShow() {
    if (this.data.orderNo) {
      this.loadOrder();
    }
  },

  onPullDownRefresh() {
    this.loadOrder().finally(() => wx.stopPullDownRefresh());
  },

  async loadOrder() {
    try {
      const order = await getOrder(this.data.orderNo);
      const items = (order.items || []).map((item) => {
        let specText = '';
        if (item.specSnapshot) {
          try {
            specText = Object.values(JSON.parse(item.specSnapshot)).join(' / ');
          } catch (error) {
            specText = item.specSnapshot;
          }
        }
        return {
          ...item,
          specText,
          priceText: formatMoney(item.unitPrice),
          lineTotalText: formatMoney(item.payableTotal),
        };
      });
      let addressText = '';
      if (order.addressSnapshot) {
        try {
          const address = JSON.parse(order.addressSnapshot);
          addressText = [
            address.recipientName,
            address.mobile,
            address.province,
            address.city,
            address.district,
            address.addressDetail,
          ]
            .filter(Boolean)
            .join(' ');
        } catch (error) {
          addressText = order.addressSnapshot;
        }
      }
      let hasShipment = false;
      let review: ReviewView | null = null;
      if (order.orderType === 'PRODUCT'
          && ['SHIPPED', 'COMPLETED'].includes(order.orderStatus)) {
        hasShipment = await getShipment(order.orderNo)
          .then(() => true)
          .catch(() => false);
      }
      if (order.orderStatus === 'COMPLETED') {
        review = await getOrderReview(order.orderNo).catch(() => null);
      }
      const canAfterSale =
        order.orderType === 'PRODUCT'
        && ['PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED'].includes(order.orderStatus)
        && (order.paidAmount || 0) > (order.refundedAmount || 0);
      this.setData({
        order,
        statusText: STATUS_TEXT[order.orderStatus] || order.orderStatus,
        paymentStatusText: PAYMENT_TEXT[order.paymentStatus] || order.paymentStatus,
        items,
        addressText,
        originalText: formatMoney(order.originalAmount),
        discountText: formatMoney(order.discountAmount),
        payableText: formatMoney(order.payableAmount),
        paidText: formatMoney(order.paidAmount),
        canPay: order.orderStatus === 'WAIT_PAY',
        canCancel: order.orderStatus === 'WAIT_PAY',
        hasShipment,
        canAfterSale,
        review,
        paidTimeText: order.paidTime ? formatDateTime(order.paidTime) : '',
        paymentChannelText: order.latestPayment
          ? `${order.latestPayment.payChannel} · ${order.latestPayment.status}`
          : '',
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  choosePay() {
    if (this.data.paying) return;
    wx.showActionSheet({
      itemList: ['钱包余额支付', '模拟微信支付（开发环境）'],
      success: async (result) => {
        const channel = result.tapIndex === 0 ? 'WALLET' : 'MOCK_WECHAT';
        await this.pay(channel);
      },
    });
  },

  async pay(channel: string) {
    try {
      this.setData({ paying: true });
      await payOrder(this.data.orderNo, channel);
      wx.showToast({ title: '支付成功', icon: 'success' });
      await this.loadOrder();
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ paying: false });
    }
  },

  cancelOrder() {
    wx.showModal({
      title: '取消订单',
      content: '确定取消该订单吗？库存和优惠券会释放。',
      success: async (result) => {
        if (!result.confirm) return;
        try {
          await cancelOrder(this.data.orderNo, '用户取消');
          wx.showToast({ title: '订单已取消', icon: 'success' });
          await this.loadOrder();
        } catch (error) {
          // request 已提示
        }
      },
    });
  },

  confirmReceipt() {
    wx.showModal({
      title: '确认收货',
      content: '确认已收到商品吗？',
      success: async (result) => {
        if (!result.confirm) return;
        try {
          await confirmReceipt(this.data.orderNo);
          wx.showToast({ title: '已确认收货', icon: 'success' });
          await this.loadOrder();
        } catch (error) {
          // request 已提示
        }
      },
    });
  },

  goShipment() {
    wx.navigateTo({
      url: `/pages/commerce/shipment/index?orderNo=${this.data.orderNo}`,
    });
  },

  goAfterSale() {
    wx.navigateTo({
      url: `/pages/commerce/after-sale/apply?orderNo=${this.data.orderNo}`,
    });
  },

  goAfterSaleList() {
    wx.navigateTo({ url: '/pages/commerce/after-sale/list' });
  },

  goReview() {
    wx.navigateTo({
      url: `/pages/commerce/review/index?orderNo=${this.data.orderNo}`,
    });
  },

  goOrders() {
    wx.redirectTo({ url: '/pages/commerce/order/index' });
  },
});
