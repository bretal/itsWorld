import { requireLogin } from '../../../utils/auth';
import {
  deleteAddress,
  listAddresses,
  setDefaultAddress,
} from '../../../services/commerce';
import type { ShippingAddress } from '../../../services/commerce';

interface AddressView extends ShippingAddress {
  fullAddress: string;
}

Page({
  data: {
    addresses: [] as AddressView[],
    loading: true,
  },

  onShow() {
    if (!requireLogin()) {
      return;
    }
    this.loadAddresses();
  },

  onPullDownRefresh() {
    this.loadAddresses().finally(() => wx.stopPullDownRefresh());
  },

  async loadAddresses() {
    try {
      const addresses = await listAddresses();
      this.setData({
        addresses: addresses.map((address) => ({
          ...address,
          fullAddress: `${address.province || ''}${address.city || ''}${address.district || ''}${address.addressDetail}`,
        })),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  goAdd() {
    wx.navigateTo({ url: '/pages/commerce/address/edit' });
  },

  goEdit(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    wx.navigateTo({ url: `/pages/commerce/address/edit?id=${id}` });
  },

  async setDefault(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    try {
      await setDefaultAddress(id);
      await this.loadAddresses();
    } catch (error) {
      // request 已提示
    }
  },

  remove(event: any) {
    const id = Number(event.currentTarget.dataset.id);
    wx.showModal({
      title: '删除地址',
      content: '确定删除该收货地址吗？',
      success: async (result) => {
        if (!result.confirm) return;
        try {
          await deleteAddress(id);
          await this.loadAddresses();
        } catch (error) {
          // request 已提示
        }
      },
    });
  },
});
