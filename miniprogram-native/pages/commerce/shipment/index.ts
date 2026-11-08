import { requireLogin } from '../../../utils/auth';
import { confirmReceipt, getShipment } from '../../../services/commerce';
import type { ShipmentView } from '../../../services/commerce';
import { formatDateTime } from '../../../utils/format';

Page({
  data: {
    orderNo: '',
    loading: true,
    confirming: false,
    shipment: null as ShipmentView | null,
    statusText: '',
    shippedText: '',
    receivedText: '',
    canConfirm: false,
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    this.setData({ orderNo: options.orderNo || '' });
  },

  onShow() {
    if (this.data.orderNo) {
      this.load();
    }
  },

  async load() {
    try {
      const shipment = await getShipment(this.data.orderNo);
      this.setData({
        shipment,
        statusText: shipment.status === 'DELIVERED' ? '已签收' : '运输中',
        shippedText: shipment.shippedTime ? formatDateTime(shipment.shippedTime) : '',
        receivedText: shipment.receivedTime ? formatDateTime(shipment.receivedTime) : '',
        canConfirm: shipment.status === 'SHIPPED',
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  confirm() {
    wx.showModal({
      title: '确认收货',
      content: '确认已收到商品吗？',
      success: async (result) => {
        if (!result.confirm) return;
        try {
          this.setData({ confirming: true });
          await confirmReceipt(this.data.orderNo);
          wx.showToast({ title: '已确认收货', icon: 'success' });
          await this.load();
        } catch (error) {
          // request 已提示
        } finally {
          this.setData({ confirming: false });
        }
      },
    });
  },

  goOrder() {
    wx.navigateTo({
      url: `/pages/commerce/order/detail?orderNo=${this.data.orderNo}`,
    });
  },
});
