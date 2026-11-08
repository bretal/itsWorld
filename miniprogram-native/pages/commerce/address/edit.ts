import { requireLogin } from '../../../utils/auth';
import {
  createAddress,
  listAddresses,
  updateAddress,
} from '../../../services/commerce';
import type { AddressSaveRequest } from '../../../services/commerce';

Page({
  data: {
    id: 0,
    loading: false,
    saving: false,
    form: {
      recipientName: '',
      mobile: '',
      province: '',
      city: '',
      district: '',
      addressDetail: '',
      isDefault: 1,
    } as AddressSaveRequest,
  },

  onLoad(options: any) {
    if (!requireLogin()) {
      return;
    }
    const id = Number(options.id || 0);
    this.setData({ id });
    if (id) {
      this.loadAddress(id);
    }
  },

  async loadAddress(id: number) {
    try {
      const addresses = await listAddresses();
      const address = addresses.find((item) => item.id === id);
      if (!address) {
        wx.showToast({ title: '地址不存在', icon: 'none' });
        return;
      }
      this.setData({
        form: {
          recipientName: address.recipientName,
          mobile: address.mobile,
          province: address.province || '',
          city: address.city || '',
          district: address.district || '',
          addressDetail: address.addressDetail,
          isDefault: address.isDefault,
        },
      });
    } catch (error) {
      // request 已提示
    }
  },

  onInput(event: any) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: event.detail.value });
  },

  onDefaultChange(event: any) {
    this.setData({ 'form.isDefault': event.detail.value ? 1 : 0 });
  },

  async save() {
    const form = this.data.form;
    if (!form.recipientName.trim()) {
      wx.showToast({ title: '请填写收货人', icon: 'none' });
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(form.mobile)) {
      wx.showToast({ title: '请填写正确手机号', icon: 'none' });
      return;
    }
    if (!form.addressDetail.trim()) {
      wx.showToast({ title: '请填写详细地址', icon: 'none' });
      return;
    }
    try {
      this.setData({ saving: true });
      if (this.data.id) {
        await updateAddress(this.data.id, form);
      } else {
        await createAddress(form);
      }
      wx.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 400);
    } catch (error) {
      // request 已提示
    } finally {
      this.setData({ saving: false });
    }
  },
});
