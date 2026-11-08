import { requireLogin } from '../../../utils/auth';
import { listPets } from '../../../services/pet';
import {
  createBooking,
  getGroomingService,
  listBookingSlots,
  listPackageBookingOptions,
} from '../../../services/commerce';
import type {
  BookingSlot,
  GroomingServiceDetail,
  PackageBookingOption,
} from '../../../services/commerce';
import type { Pet } from '../../../services/pet';
import { formatMoney, todayString } from '../../../utils/format';

Page({
  data: {
    serviceId: 0,
    storeId: 0,
    loading: true,
    submitting: false,
    detail: null as GroomingServiceDetail | null,
    priceText: '0.00',
    memberPriceText: '',
    payableText: '0.00',
    pets: [] as Pet[],
    petNames: [] as string[],
    petIndex: 0,
    date: todayString(),
    slots: [] as BookingSlot[],
    slotTexts: [] as string[],
    selectedSlotId: 0,
    payType: 'ONLINE' as 'ONLINE' | 'PACKAGE',
    packageOptions: [] as PackageBookingOption[],
    packageLabels: [] as string[],
    packageIndex: 0,
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const serviceId = Number(options.serviceId || 0);
    const storeId = Number(options.storeId || 0);
    this.setData({ serviceId, storeId });
    this.loadAll();
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh());
  },

  async loadAll() {
    try {
      await this.loadPets();
      await this.loadDetail();
      await this.loadSlots();
      await this.loadPackageOptions();
      this.setData({ loading: false });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  async loadPets() {
    const pets = await listPets();
    let petIndex = 0;
    const storedPetId = Number(wx.getStorageSync('currentPetId') || 0);
    if (storedPetId) {
      const found = pets.findIndex((pet) => pet.id === storedPetId);
      if (found >= 0) {
        petIndex = found;
      }
    }
    this.setData({
      pets,
      petNames: pets.map((pet) => pet.petName),
      petIndex,
    });
  },

  async loadDetail() {
    const pet = this.data.pets[this.data.petIndex];
    const detail = await getGroomingService(
      this.data.serviceId,
      this.data.storeId || undefined,
      pet?.id,
    );
    const price = detail.storePrice ?? detail.basePrice;
    const memberPrice = detail.memberPrice ?? price;
    this.setData({
      detail,
      priceText: formatMoney(price),
      memberPriceText:
        memberPrice < price ? `会员价 ¥${formatMoney(memberPrice)}` : '',
      payableText: formatMoney(memberPrice),
    });
  },

  async loadSlots() {
    const slots = await listBookingSlots(this.data.storeId, this.data.date);
    this.setData({
      slots: slots.map((slot) => ({
        ...slot,
        startText: String(slot.startTime).slice(0, 5),
        endText: String(slot.endTime).slice(0, 5),
      })) as any[],
      selectedSlotId: 0,
    });
  },

  async loadPackageOptions() {
    const options = await listPackageBookingOptions(
      this.data.serviceId,
      this.data.storeId || undefined,
    );
    this.setData({
      packageOptions: options,
      packageLabels: options.map(
        (option) =>
          `${option.packageName} · 余${option.remainingTimes}次${option.expireTime ? ' · 到期' + option.expireTime.slice(0, 10) : ''}`,
      ),
      packageIndex: 0,
    });
  },

  async onPetChange(event: any) {
    const petIndex = Number(event.detail.value);
    this.setData({ petIndex });
    try {
      await this.loadDetail();
    } catch (error) {
      // request 已提示
    }
  },

  async onDateChange(event: any) {
    const date = event.detail.value;
    this.setData({ date });
    try {
      await this.loadSlots();
    } catch (error) {
      // request 已提示
    }
  },

  onSelectSlot(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    this.setData({ selectedSlotId: id });
  },

  onPayTypeChange(event: any) {
    this.setData({ payType: event.currentTarget.dataset.type });
  },

  onPackageChange(event: any) {
    this.setData({ packageIndex: Number(event.detail.value) });
  },

  goPets() {
    wx.navigateTo({ url: '/pages/pet/edit' });
  },

  async submit() {
    if (this.data.submitting) {
      return;
    }
    const pet = this.data.pets[this.data.petIndex];
    if (!pet) {
      wx.showToast({ title: '请先添加宠物', icon: 'none' });
      return;
    }
    if (!this.data.selectedSlotId) {
      wx.showToast({ title: '请选择预约时段', icon: 'none' });
      return;
    }
    let userPackageItemId: number | undefined;
    if (this.data.payType === 'PACKAGE') {
      const option = this.data.packageOptions[this.data.packageIndex];
      if (!option) {
        wx.showToast({ title: '没有可用于该服务的套餐次数', icon: 'none' });
        return;
      }
      userPackageItemId = option.userPackageItemId;
    }
    if (this.data.detail?.petEligible === false) {
      wx.showToast({
        title: this.data.detail.ineligibleReason || '宠物不符合服务条件',
        icon: 'none',
      });
      return;
    }
    try {
      this.setData({ submitting: true });
      const result = await createBooking({
        storeId: this.data.storeId,
        serviceId: this.data.serviceId,
        petId: pet.id,
        slotId: this.data.selectedSlotId,
        payType: this.data.payType,
        userPackageItemId,
        idempotencyKey: `BK-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
      });
      if (result.orderNo) {
        wx.showToast({ title: '预约已创建，请支付', icon: 'none' });
        wx.redirectTo({
          url: `/pages/commerce/order/detail?orderNo=${result.orderNo}`,
        });
      } else {
        wx.showToast({ title: '预约已确认', icon: 'success' });
        wx.redirectTo({
          url: `/pages/commerce/booking/detail?bookingNo=${result.bookingNo}`,
        });
      }
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ submitting: false });
    }
  },
});
