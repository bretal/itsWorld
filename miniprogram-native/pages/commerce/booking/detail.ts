import { requireLogin } from '../../../utils/auth';
import {
  cancelBooking,
  getBooking,
  listBookingSlots,
  payOrder,
  rescheduleBooking,
} from '../../../services/commerce';
import type { BookingView } from '../../../services/commerce';
import { formatDateTime, todayString } from '../../../utils/format';

const STATUS_TEXT: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  CONFIRMED: '已确认',
  CHECKED_IN: '已签到',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
};

Page({
  data: {
    bookingNo: '',
    loading: true,
    paying: false,
    booking: null as BookingView | null,
    statusText: '',
    dateText: '',
    timeText: '',
    createdAtText: '',
    checkinText: '',
    completedText: '',
    logs: [] as any[],
    canPay: false,
    canCancel: false,
    canReschedule: false,
    showReschedule: false,
    rescheduleDate: todayString(),
    rescheduleSlots: [] as any[],
    selectedSlotId: 0,
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    this.setData({ bookingNo: options.bookingNo || '' });
  },

  onShow() {
    if (this.data.bookingNo) {
      this.load();
    }
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh());
  },

  async load() {
    try {
      const booking = await getBooking(this.data.bookingNo);
      this.setData({
        booking,
        statusText: STATUS_TEXT[booking.status] || booking.status,
        dateText: booking.serviceDate || '',
        timeText:
          booking.startTime && booking.endTime
            ? `${String(booking.startTime).slice(0, 5)}-${String(booking.endTime).slice(0, 5)}`
            : '',
        createdAtText: formatDateTime(booking.createdAt),
        checkinText: booking.checkinTime
          ? formatDateTime(booking.checkinTime)
          : '',
        completedText: booking.completedTime
          ? formatDateTime(booking.completedTime)
          : '',
        logs: (booking.logs || []).map((log) => ({
          ...log,
          createdAtText: formatDateTime(log.createdAt),
        })),
        canPay: booking.status === 'PENDING_PAYMENT' && !!booking.orderNo,
        canCancel: booking.status === 'PENDING_PAYMENT' && !!booking.orderNo,
        canReschedule:
          booking.status === 'PENDING_PAYMENT' || booking.status === 'CONFIRMED',
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  choosePay() {
    if (this.data.paying || !this.data.booking?.orderNo) return;
    wx.showActionSheet({
      itemList: ['钱包余额支付', '模拟微信支付（开发环境）'],
      success: async (result) => {
        const channel = result.tapIndex === 0 ? 'WALLET' : 'MOCK_WECHAT';
        await this.pay(channel);
      },
    });
  },

  async pay(channel: string) {
    if (!this.data.booking?.orderNo) return;
    try {
      this.setData({ paying: true });
      await payOrder(this.data.booking.orderNo, channel);
      wx.showToast({ title: '支付成功', icon: 'success' });
      await this.load();
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ paying: false });
    }
  },

  cancel() {
    wx.showModal({
      title: '取消预约',
      content: '确定取消本次预约吗？时段容量和优惠券会释放。',
      success: async (result) => {
        if (!result.confirm) return;
        try {
          await cancelBooking(this.data.bookingNo, '用户取消预约');
          wx.showToast({ title: '预约已取消', icon: 'success' });
          await this.load();
        } catch (error) {
          // request 已提示
        }
      },
    });
  },

  async toggleReschedule() {
    const showReschedule = !this.data.showReschedule;
    this.setData({ showReschedule });
    if (showReschedule) {
      await this.loadRescheduleSlots(this.data.rescheduleDate);
    }
  },

  async onRescheduleDateChange(event: any) {
    const date = event.detail.value;
    this.setData({ rescheduleDate: date });
    await this.loadRescheduleSlots(date);
  },

  async loadRescheduleSlots(date: string) {
    if (!this.data.booking) return;
    try {
      const slots = await listBookingSlots(this.data.booking.storeId, date);
      this.setData({
        rescheduleSlots: slots.map((slot) => ({
          ...slot,
          startText: String(slot.startTime).slice(0, 5),
          endText: String(slot.endTime).slice(0, 5),
        })) as any[],
        selectedSlotId: 0,
      });
    } catch (error) {
      // request 已提示
    }
  },

  onSelectRescheduleSlot(event: any) {
    this.setData({ selectedSlotId: Number(event.currentTarget.dataset.id) });
  },

  async confirmReschedule() {
    if (!this.data.selectedSlotId) {
      wx.showToast({ title: '请选择新时段', icon: 'none' });
      return;
    }
    try {
      await rescheduleBooking(this.data.bookingNo, this.data.selectedSlotId);
      wx.showToast({ title: '已改期', icon: 'success' });
      this.setData({ showReschedule: false });
      await this.load();
    } catch (error) {
      // request 已提示
    }
  },

  goOrder() {
    if (this.data.booking?.orderNo) {
      wx.navigateTo({
        url: `/pages/commerce/order/detail?orderNo=${this.data.booking.orderNo}`,
      });
    }
  },

  goList() {
    wx.redirectTo({ url: '/pages/commerce/booking/list' });
  },
});
