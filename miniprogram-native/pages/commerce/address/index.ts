import { requireLogin } from '../../../utils/auth';
import {
  deleteAddress,
  listAddresses,
  setDefaultAddress,
} from '../../../services/commerce';
import type { ShippingAddress } from '../../../services/commerce';

interface AddressView extends ShippingAddress {
  region: string;
}

Page({
  data: {
    addresses: [] as AddressView[],
    loading: true,
    loadFailed: false,
    selecting: false,
    selectedId: 0,
    returning: false,
  },

  onLoad(options: { from?: string; selectedId?: string }) {
    this.setData({
      selecting: options.from === 'checkout',
      selectedId: Number(options.selectedId) || 0,
    });
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
    this.setData({ loading: true, loadFailed: false });
    try {
      const addresses = await listAddresses();
      this.setData({
        addresses: addresses.map((address) => ({
          ...address,
          region: [...new Set([address.province, address.city, address.district].filter(Boolean))].join(' · '),
        })),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false, loadFailed: true });
    }
  },

  selectAddress(event: { currentTarget: { dataset: { id: number } } }) {
    if (!this.data.selecting || this.data.returning) return;
    const address = this.data.addresses.find((item) => item.id === Number(event.currentTarget.dataset.id));
    if (!address) return;
    this.setData({ returning: true });
    this.getOpenerEventChannel().emit('addressSelected', address.id);
    wx.navigateBack({ fail: () => this.setData({ returning: false }) });
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
